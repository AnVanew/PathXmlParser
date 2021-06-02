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
    Map<Field, Set> fieldSetMap = new HashMap<>();

    Map<Object, Field> objectInnerSetMap = new HashMap<>();
    Map<String, Field> innerSetPathFieldMap = new HashMap<>();

    Map<Field, Object> innerObjectMap = new HashMap<>();
    Map<Object, Object> outObjectMap = new HashMap<>();
    Map<String, Field> setElements = new HashMap<>();

    Stack<String> paths = new Stack<>();
    Stack<String> currentTagStack = new Stack<>();
    Stack<Object> currentObjectStack = new Stack<>();

    Object t;

    public <T> T parseFromXml(InputStream inputStream, Class<?> T) {

        try {
            t = Class.forName(T.getName()).newInstance();
            currentObjectStack.push(t);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        getXmlPathFieldsMap(T);

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();
            saxParser.parse(inputStream, xmlHandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        setCompleteObjectFields();

        return (T) t;
    }

    private void setCompleteObjectFields(){

        for (Map.Entry<Object, Field> entry : objectInnerSetMap.entrySet()){
            setObjectField(entry.getKey(), entry.getValue(), fieldSetMap.get(entry.getValue()));
        }

        for (Map.Entry<Object, Object> entry : outObjectMap.entrySet()) {
            Field[] field = entry.getKey().getClass().getFields();
            for (Field parentField : field) {
                if (parentField.isAnnotationPresent(XmlInnerClass.class)) {
                    setObjectField(entry.getKey(), parentField, entry.getValue());
                }
            }
        }

        for (Field field : fieldSetMap.keySet()){
            if (!field.isAnnotationPresent(XmlInnerSet.class)) setObjectField(t, field, fieldSetMap.get(field));
        }
    }

    private void setObjectField(Object o, Field field, Object value){
        try {
            field.set(o, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void getXmlPathFieldsMap(Class<?> T) {

        Field[] fields = T.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
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
                    setElements.put(endPath, field);
                }

                Class<?> innerClass = field.getAnnotation(XmlSet.class).setClass();
                if (!fieldSetMap.containsKey(field)) {
                    fieldSetMap.put(field, createNewHashSet(innerClass));
                }

            }

            if (field.isAnnotationPresent(XmlInnerSet.class)){
                innerSetPathFieldMap.put(field.getAnnotation(XmlInnerSet.class).path(), field);
            }

            if (field.isAnnotationPresent(XmlInnerClass.class)){
                for (String path : field.getAnnotation(XmlInnerClass.class).path()){
                    pathFieldMap.put(path, field);
                }
                getXmlPathFieldsMap(field.getType());
            }
        }
    }

    private <T> Set<T> createNewHashSet (Class<T> t){
        return new HashSet<T>();
    }

    class XmlHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

            paths.push(qName);

            //Если наткнулись на внутренний класс
            if(pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlInnerClass.class)){

                currentTagStack.push(qName);

                Class<?> innerClass = pathFieldMap.get(getCurrentPath()).getType();

                try {
                    Object obj = Class.forName(innerClass.getName()).newInstance();

                    outObjectMap.put(currentObjectStack.peek(), obj);
                    innerObjectMap.put(pathFieldMap.get(getCurrentPath()), obj);

                    currentObjectStack.push(obj);
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            //Если наткнулись на элемент set-a
            if (setElements.containsKey(getCurrentPath())){

                String[] elements = setElements.get(getCurrentPath()).getAnnotation(XmlSetElement.class).elements();

                Class<?> innerClass = setElements.get(getCurrentPath()).getAnnotation(XmlSet.class).setClass();
                for (String element : elements) {
                    if (element.equals(getCurrentPath())) {
                        try {
                            Object obj = Class.forName(innerClass.getName()).newInstance();
                            fieldSetMap.get(setElements.get(getCurrentPath())).add(obj);

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

            //Если наткнулись на внутренний Set
            if(innerSetPathFieldMap.containsKey(getCurrentPath())) {
                objectInnerSetMap.put(currentObjectStack.peek(), innerSetPathFieldMap.get(getCurrentPath()));
            }

            // Если наткнулись на элемент с атрибутами
            if (attributes.getLength() != 0){
                setFieldByAttribute(attributes);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String information = new String(ch, start, length).replace("\n", "").trim();
            if (!information.isEmpty()) setField(information, getCurrentPath());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            if (!currentTagStack.isEmpty() && qName.equals(currentTagStack.peek())){
                currentObjectStack.pop();
                currentTagStack.pop();
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
