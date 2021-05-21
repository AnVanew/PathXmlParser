package pathXmlParser;

public class Cat {

    @Attribute(name = "first")
    @XmlPath(path = "/cat/name")
    public String name;

    @XmlPath(path = "/cat/sex")
    public boolean sex;

    @XmlPath(path = "/cat/surname")
    public String surname;
}
