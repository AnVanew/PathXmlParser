package pathXmlParser;

public class Cat {


    @XmlPath(path = "/cat/name:first")
    public String name;

    @XmlPath(path =  "/cat/name")
    public String nameFuck;

    @XmlPath(path = "/cat/sex")
    public boolean sex;

    @XmlPath(path = "/cat/surname")
    public String surname;
}
