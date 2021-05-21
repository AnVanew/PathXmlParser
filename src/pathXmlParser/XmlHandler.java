package pathXmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Iterator;
import java.util.Stack;

public class XmlHandler extends DefaultHandler {

    Stack<String> paths = new Stack<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        paths.push(qName);

            StringBuilder path = new StringBuilder();
            Iterator iterator = paths.iterator();
            while (iterator.hasNext()) {
                path.append("/" + iterator.next());
            }
            System.out.println(path);


    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        paths.pop();
    }


}
