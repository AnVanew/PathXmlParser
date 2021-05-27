import pathXmlParser.Cat;
import pathXmlParser.Kitty;
import pathXmlParser.PathXmlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        FileInputStream file = new FileInputStream(new File("cat.xml"));
        PathXmlParser pathXmlParser = new PathXmlParser();
        Cat cat = pathXmlParser.parseFromXml(file, Cat.class);
        for (Kitty kitty :  cat.kittySet){
            System.out.println(kitty.name);
            System.out.println(kitty.momName);
        }
    }
}
