package pathXmlParser;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class PathXmlParser {

    public void parseFromXml(InputStream inputStream, Class<?> T){

        Map<String, Field> fieldMap = getXmlPathFieldsMap(T);

        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            XmlHandler xmlHandler = new XmlHandler();

            saxParser.parse(inputStream, xmlHandler);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private  Map<String, Field> getXmlPathFieldsMap(Class<?> T){

        Map<String, Field> fieldMap = new HashMap<>();
        Field[] fields = T.getDeclaredFields();

        for (Field field: fields){
            if (field.isAnnotationPresent(XmlPath.class)){
                fieldMap.put(field.getAnnotation(XmlPath.class).path(), field);
            }
        }

        return fieldMap;
    }
}
