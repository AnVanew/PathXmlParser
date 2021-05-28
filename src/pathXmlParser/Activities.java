package pathXmlParser;

public class Activities {
    @XmlPath(path = "/cat/sons/son/activity:name")
    public String name;

    @XmlPath(path = "/cat/sons/son/activity:count")
    public Integer count;

    public String type;
}
