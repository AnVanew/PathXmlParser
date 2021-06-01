package pathXmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;

public class PathXmlParser {

    Map<String, Field> pathFieldMap = new HashMap<>();
    Map<Field, Set> fieldSetMap;
    Map<Field, Object> innerObjectMap = new HashMap<>();
    Map<Object, Object> outObjectMap = new HashMap<>();

    Stack<String> paths = new Stack<>();
    Stack<Object> currentObjectStack = new Stack<>();
    Stack<String> currentTagStack = new Stack<>();
    Object t;

    public <T> T parseFromXml(InputStream inputStream, Class<?> T) {

        //TODO
        t = (T) new Cat();
        currentObjectStack.push(t);

        getXmlPathFieldsMap(T);
        fieldSetMap = new HashMap<>();

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();

            saxParser.parse(inputStream, xmlHandler);

            setCompleteObjectFields();

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return (T) t;


    }

    private void setCompleteObjectFields(){
        for (Map.Entry<Object, Object> entry : outObjectMap.entrySet()) {
            Field[] field = entry.getKey().getClass().getFields();
            for (Field field1 : field) {
                if (field1.isAnnotationPresent(XmlInnerClass.class)) {
                    try {
                        field1.set(entry.getKey(), entry.getValue());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        for (Field field : fieldSetMap.keySet()){
            try {
                field.set(t, fieldSetMap.get(field));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void getXmlPathFieldsMap(Class<?> T) {

        Field[] fields = T.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(XmlPath.class)) {
                pathFieldMap.put(field.getAnnotation(XmlPath.class).path(), field);
            }
            else if (field.isAnnotationPresent(XmlMultiPath.class)){
                for (String path : field.getAnnotation(XmlMultiPath.class).path()){
                    pathFieldMap.put(path, field);
                }
            }
            if (field.isAnnotationPresent(XmlSet.class)){
                getXmlPathFieldsMap(field.getAnnotation(XmlSet.class).setClass());
                for (String endPath : field.getAnnotation(XmlSetElement.class).elements()){
                    pathFieldMap.put(endPath, field);
                }
            }
            if (field.isAnnotationPresent(XmlInnerClass.class)){
                for (String path : field.getAnnotation(XmlInnerClass.class).path()){
                    pathFieldMap.put(path, field);
                }
                getXmlPathFieldsMap(field.getType());
            }
        }
    }

    class XmlHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            paths.push(qName);



            //Если наткнулись на внутренний класс
            if(pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlInnerClass.class)){

                currentTagStack.push(qName);

                Class<?> innerClass = pathFieldMap.get(getCurrentPath()).getType();

                Object obj = null;
                try {
                    obj = Class.forName(innerClass.getName()).newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                outObjectMap.put(currentObjectStack.peek(), obj);
                innerObjectMap.put(pathFieldMap.get(getCurrentPath()), obj);

                currentObjectStack.push(obj);
            }




            //Если наткнулись на Set
            if(pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlSet.class)) {

                Class<?> innerClass = pathFieldMap.get(getCurrentPath()).getAnnotation(XmlSet.class).setClass();

                if (!fieldSetMap.containsKey(pathFieldMap.get(getCurrentPath()))) {
                    fieldSetMap.put(pathFieldMap.get(getCurrentPath()), createNewHashSet(innerClass));
                }
                else {
                String[] elements = pathFieldMap.get(getCurrentPath()).getAnnotation(XmlSetElement.class).elements();

                for (String element : elements) {
                    if (element.equals(getCurrentPath())) {
                        try {
                            Object obj = Class.forName(innerClass.getName()).newInstance();
                            fieldSetMap.get(pathFieldMap.get(getCurrentPath())).add(obj);
                            currentObjectStack.push(obj);
                            currentTagStack.push(qName);

                            //TODO constants
                            try {
                                innerClass.getField("type").set(obj, getCurrentPath());
                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            }

                        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
                }
            }


            // Если элемент с атрибутами
            if (attributes.getLength() != 0){
                setFieldByAttribute(attributes);
            }
        }

        private <T> Set<T> createNewHashSet (Class<T> t){
            return new HashSet<T>();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String information = new String(ch, start, length).replace("\n", "").trim();
            if (!information.isEmpty()) setField(information, getCurrentPath());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            if (!currentTagStack.isEmpty() && qName.equals(currentTagStack.peek())){
//                System.out.println(getCurrentPath());
//                System.out.println(currentObjectStack + " STACK before");
                currentObjectStack.pop();
                currentTagStack.pop();
//                System.out.println(currentObjectStack + " STACK after");
//                System.out.println("--------------------------------");
            }

            paths.pop();
        }

        private String getCurrentPath() {
            StringBuilder path = new StringBuilder();

            for (String s : paths) {
                path.append("/").append(s);
            }
            return path.toString();
        }

        private void setFieldByAttribute(Attributes attributes){
            for (int i = 0; i < attributes.getLength(); i++) {
                setField(attributes.getValue(i), getCurrentPath() + ":" + attributes.getQName(i));
            }
        }

        private void setField(String information, String path){
            if (pathFieldMap.containsKey(path)) {
                try {
                    Field field = pathFieldMap.get(path);
                    field.set(currentObjectStack.peek(), convert(field.getType(), information));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        private <T> T convert(Class<?> T, String text) {
            PropertyEditor editor = PropertyEditorManager.findEditor(T);
            editor.setAsText(text);
            return (T) editor.getValue();
        }
    }
}
