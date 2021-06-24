package pathXmlParser;

import lombok.extern.slf4j.Slf4j;
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
import java.lang.reflect.ParameterizedType;
import java.util.*;

@Slf4j
public class PathXmlParser {

    private Map<String, Field> pathFieldMap = new HashMap<>();
    private Map<Field, Set> fieldSetMap = new HashMap<>();

    private Map<Object, Field> objectInnerSetMap = new HashMap<>();
    private Map<String, Field> innerSetPathFieldMap = new HashMap<>();

    private Map<Field, Object> innerObjectMap = new HashMap<>();
    private Map<Object, Object> outObjectMap = new HashMap<>();
    private Map<String, Field> setElements = new HashMap<>();

    private Stack<String> paths = new Stack<>();
    private Stack<String> currentTagStack = new Stack<>();
    private Stack<Object> currentObjectStack = new Stack<>();

    private String root = "";
    private boolean isStarted = true;

    private TypeNumbersConstants typeNumbersConstants = new TypeNumbersConstants();

    public <T> T parseFromXml(InputStream inputStream, Class<?> T) throws XmlParseException {

        Object t = null;

        if (T.isAnnotationPresent(XmlRoot.class)){
            root = T.getAnnotation(XmlRoot.class).path();
            isStarted = false;
        }

        try {
            t = Class.forName(T.getName()).newInstance();
            currentObjectStack.push(t);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error(e.toString());
            throw new XmlParseException();
        }

        getXmlPathFieldsMap(T);

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();
            saxParser.parse(inputStream, xmlHandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.toString());
            throw new XmlParseException();
        }

        setCompleteObjectFields(t);

        return (T) t;
    }

    private void setCompleteObjectFields(Object t){
        for (Map.Entry<Object, Object> entry : outObjectMap.entrySet()) {
            Field[] field = entry.getKey().getClass().getDeclaredFields();
            for (Field parentField : field) {
                if (parentField.isAnnotationPresent(XmlInnerClass.class) && parentField.getType().equals(entry.getValue().getClass())) {
                    parentField.setAccessible(true);
                    setObjectField(entry.getKey(), parentField, entry.getValue());
                }
            }
        }

        for (Map.Entry<Object, Field> entry : objectInnerSetMap.entrySet()){
            setObjectField(entry.getKey(), entry.getValue(), fieldSetMap.get(entry.getValue()));
        }

        for (Field field : fieldSetMap.keySet()){
            if (!field.isAnnotationPresent(XmlInnerSet.class)) setObjectField(t, field, fieldSetMap.get(field));
        }
    }

    private void setObjectField(Object o, Field field, Object value){
        try {
            field.set(o, value);
        } catch (IllegalAccessException e) {
            log.error("Could not set value of field");
            log.error(e.toString());
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

                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                Class<?> innerClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];

                getXmlPathFieldsMap(innerClass);
                for (String endPath : field.getAnnotation(XmlSet.class).elements()){
                    setElements.put(endPath, field);
                }

                if (!fieldSetMap.containsKey(field)) {
                    fieldSetMap.put(field, createNewHashSet(innerClass));
                }
            }

            if (field.isAnnotationPresent(XmlInnerSet.class)){
                for (String path : field.getAnnotation(XmlInnerSet.class).path()){
                    innerSetPathFieldMap.put(path, field);
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

    private <T> Set<T> createNewHashSet (Class<T> t){
        return new HashSet<T>();
    }

    class XmlHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (isStarted) {
                paths.push(qName);

                if (!currentTagStack.isEmpty() && qName.equalsIgnoreCase(currentTagStack.peek())){
                    currentTagStack.push(qName);
                }

                //Если наткнулись на внутренний класс
                if (pathFieldMap.containsKey(getCurrentPath()) && pathFieldMap.get(getCurrentPath()).isAnnotationPresent(XmlInnerClass.class)) {

                    currentTagStack.push(qName);

                    Class<?> innerClass = pathFieldMap.get(getCurrentPath()).getType();

                    try {
                        Object obj = Class.forName(innerClass.getName()).newInstance();

                        outObjectMap.put(currentObjectStack.peek(), obj);
                        innerObjectMap.put(pathFieldMap.get(getCurrentPath()), obj);
                        currentObjectStack.push(obj);

                    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                        log.error(e.toString());;
                    }
                }

                //Если наткнулись на элемент set-a
                if (setElements.containsKey(getCurrentPath())) {

                    String[] elements = setElements.get(getCurrentPath()).getAnnotation(XmlSet.class).elements();

                    ParameterizedType parameterizedType = (ParameterizedType) setElements.get(getCurrentPath()).getGenericType();
                    Class<?> innerClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];


                    for (String element : elements) {
                        if (element.equals(getCurrentPath())) {
                            try {
                                Object obj = Class.forName(innerClass.getName()).newInstance();
                                fieldSetMap.get(setElements.get(getCurrentPath())).add(obj);

                                currentObjectStack.push(obj);
                                currentTagStack.push(qName);

                                Field[] fields = innerClass.getDeclaredFields();
                                for (Field field : fields) {
                                    field.setAccessible(true);
                                    if (field.getName().equals("type")) {
                                        setObjectField(obj, field, typeNumbersConstants.typeNumberMap.get(getCurrentPath()));
                                    }
                                }
                            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                                log.error(e.toString());
                            }
                        }
                    }
                }

                //Если наткнулись на внутренний Set
                if (innerSetPathFieldMap.containsKey(getCurrentPath())) {
                    objectInnerSetMap.put(currentObjectStack.peek(), innerSetPathFieldMap.get(getCurrentPath()));
                }

                // Если наткнулись на элемент с атрибутами
                if (attributes.getLength() != 0) {
                    setFieldByAttribute(attributes);
                }
            }
            else if (qName.equalsIgnoreCase(root)) isStarted = true;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String information = new String(ch, start, length).replace("\n", "").trim();
            if (!information.isEmpty()) setField(information, getCurrentPath());
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            //Тег может открыться два раза, но означать совсем разные вещи. (fileSet в receivingDestination)
            if (currentTagStack.size()>=2 && currentTagStack.peek().equals(currentTagStack.elementAt(currentTagStack.size()-2))) {
                currentTagStack.pop();
                paths.pop();
            }

            else {
                if (!currentTagStack.isEmpty() && qName.equals(currentTagStack.peek())) {
                    currentObjectStack.pop();
                    currentTagStack.pop();
                }

                if (!paths.isEmpty()) paths.pop();
            }
        }

        private String getCurrentPath() {
            StringBuilder path = new StringBuilder();

            for (String s : paths) {
                path.append("/").append(s);
            }
            path.deleteCharAt(0);
            return path.toString();
        }

        private void setFieldByAttribute(Attributes attributes){
            for (int i = 0; i < attributes.getLength(); i++) {
                setField(attributes.getValue(i), getCurrentPath() + ":" + attributes.getQName(i));
            }
        }

        private void setField(String information, String path){
            if (pathFieldMap.containsKey(path)) {
                Field field = pathFieldMap.get(path);
                setObjectField(currentObjectStack.peek(), field, convert(field.getType(), information));
            }
        }

        private <T> T convert(Class<?> T, String text) {
            PropertyEditor editor = PropertyEditorManager.findEditor(T);
            editor.setAsText(text);
            return (T) editor.getValue();
        }
    }
}
