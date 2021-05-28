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
    Map<Object, Field> objectFieldOfSetMap = new HashMap<>();
    Map<Field, Set> fieldSetMap;
    Map<Field, Object> innerObjectSet = new HashMap<>();

    Stack<Object> objectStack = new Stack<>();
    Stack<String> innerObjectStack = new Stack<>();
    Object t;

    public <T> T parseFromXml(InputStream inputStream, Class<?> T) {

        //TODO
        t = (T) new Cat();
        objectStack.push(t);

        getXmlPathFieldsMap(T);
        fieldSetMap = new HashMap<>();

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();

            saxParser.parse(inputStream, xmlHandler);

            createSetField();


        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return (T) t;


    }

    private void createSetField(){
        for (Map.Entry<Object, Field> entry : objectFieldOfSetMap.entrySet()){
            Field field = entry.getValue();
            try {
                field.set(entry.getKey(), fieldSetMap.get(field));
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
            }
            if (field.isAnnotationPresent(XmlInnerClass.class)){
                pathFieldMap.put(field.getAnnotation(XmlInnerClass.class).path(), field);
                getXmlPathFieldsMap(field.getType());
            }
        }
    }

    class XmlHandler extends DefaultHandler {

        Stack<String> paths = new Stack<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            paths.push(qName);

            //Если наткнулись на внутренний класс
            if(pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlInnerClass.class)){

                innerObjectStack.push(qName);

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
                innerObjectSet.put(pathFieldMap.get(getCurrentPath()), obj);
                objectStack.push(obj);
            }


            if (attributes.getLength() != 0){
                setFieldByAttribute(attributes);
            }

            //Если наткнулись на Set
            if(pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlSet.class)){

                objectFieldOfSetMap.put(objectStack.peek(), pathFieldMap.get(getCurrentPath()));

                Class<?> innerClass = pathFieldMap.get(getCurrentPath()).getAnnotation(XmlSet.class).setClass();
                if (!fieldSetMap.containsKey(pathFieldMap.get(getCurrentPath()))) {
                    fieldSetMap.put(pathFieldMap.get(getCurrentPath()), createNewHashSet(innerClass));
                }

                //TODO xzxzxz
                try {
                    Object obj = Class.forName(innerClass.getName()).newInstance();
                    fieldSetMap.get(pathFieldMap.get(getCurrentPath())).add(obj);
                    objectStack.push(obj);

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

            if (!innerObjectStack.isEmpty() && qName.equalsIgnoreCase(innerObjectStack.peek())){
                Field field = pathFieldMap.get(getCurrentPath());
                Object obj = objectStack.pop();
                try {
                    field.set(objectStack.peek(), obj);
                    Kitty k = (Kitty) objectStack.peek();
                    System.out.println(k.activity);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            paths.pop();

            if(!pathFieldMap.containsKey(getCurrentPath()) && objectStack.size()!=1){
                objectStack.pop();
            }

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
                    field.set(objectStack.peek(), convert(field.getType(), information));
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
