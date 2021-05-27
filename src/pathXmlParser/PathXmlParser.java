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

    Map<String, Field> pathFieldMap;
    Map<Field, Set> fieldSetMap;
    Stack<Object> objectStack = new Stack<>();

    Object t;

    public <T> T parseFromXml(InputStream inputStream, Class<?> T) {

        //TODO
        t = (T) new Cat();
        objectStack.push(t);

        pathFieldMap = getXmlPathFieldsMap(T);
        fieldSetMap = new HashMap<>();

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();

            saxParser.parse(inputStream, xmlHandler);

            for (Field field : fieldSetMap.keySet()){
                try {
                    field.set(t, fieldSetMap.get(field));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }


        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return (T) t;
    }


    private Map<String, Field> getXmlPathFieldsMap(Class<?> T) {

        Map<String, Field> fieldMap = new HashMap<>();
        Field[] fields = T.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(XmlPath.class)) {
                fieldMap.put(field.getAnnotation(XmlPath.class).path(), field);
            }
            else if (field.isAnnotationPresent(XmlMultiPath.class)){
                for (String path : field.getAnnotation(XmlMultiPath.class).path()){
                    fieldMap.put(path, field);
                }
            }
        }

        return fieldMap;
    }

//    private Map<Field, Set> getXmlFieldSetMap(Class<?> T) {
//
//        Map<Field, Set> fieldMap = new HashMap<>();
//        Field[] fields = T.getDeclaredFields();
//
//        for (Field field : fields) {
//            if (field.isAnnotationPresent(XmlSet.class)) {
//                Class<?> cl = field.getAnnotation(XmlSet.class).setClass();
//                if(!fieldMap.containsKey(field)) fieldMap.put(field, new HashSet<>());
//            }
//        }
//
//        return fieldMap;
//    }

    class XmlHandler extends DefaultHandler {

        Stack<String> paths = new Stack<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            paths.push(qName);

            if (attributes.getLength() != 0){
                setFieldByAttribute(attributes);
            }

            //Если наткнулись на Set
            if(pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlSet.class)){

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

                setSetField(innerClass);
            }
        }

        private <T> Set<T> createNewHashSet (Class<T> t){
            return new HashSet<T>();
        }

        private <T> void setSetField(Class<T> t){
            //TODO
            Field[] innerClassFields = t.getFields();

            for (Field field : innerClassFields){
                if (field.isAnnotationPresent(XmlPath.class)) {
                    String path = field.getAnnotation(XmlPath.class).path();
                    if (!pathFieldMap.containsKey(path)) pathFieldMap.put(path, field);
                }
                if (field.isAnnotationPresent(XmlMultiPath.class)){
                    for (String path : field.getAnnotation(XmlMultiPath.class).path()){
                        if (!pathFieldMap.containsKey(path)) pathFieldMap.put(path, field);
                    }
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String information = new String(ch, start, length).replace("\n", "").trim();
            if (!information.isEmpty()) setField(information, getCurrentPath());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
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
