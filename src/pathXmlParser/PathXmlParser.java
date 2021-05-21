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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public class PathXmlParser {

    Map<String, Field> pathFieldMap;
    Map<Field, String> attributeFieldMap;

    Object t;

    public <T> T parseFromXml(InputStream inputStream, Class<?> T) {

        //TODO
        t = (T) new Cat();

        pathFieldMap = getXmlPathFieldsMap(T);
        attributeFieldMap = getAttributeFieldsMap(T);

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();

            saxParser.parse(inputStream, xmlHandler);

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
        }

        return fieldMap;
    }

    private Map<Field, String> getAttributeFieldsMap(Class<?> T) {

        Map<Field, String> fieldMap = new HashMap<>();
        Field[] fields = T.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Attribute.class)) {
                fieldMap.put(field, field.getAnnotation(Attribute.class).name());
            }
        }

        return fieldMap;
    }

    class XmlHandler extends DefaultHandler {

        Stack<String> paths = new Stack<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            paths.push(qName);

            if (attributes.getLength() != 0){
                setFieldByAttribute(attributes);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String information = new String(ch, start, length).replace("\n", "").trim();
            setField(information);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
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
            String path = getCurrentPath();
            Field field = pathFieldMap.get(path);
            setField(attributes.getValue(attributeFieldMap.get(field)));
        }

        private void setField(String information){

            String path = getCurrentPath();
            if (pathFieldMap.containsKey(path)) {
                try {
                    Field field = pathFieldMap.get(path);
                    field.set(t, convert(field.getType(), information));
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
