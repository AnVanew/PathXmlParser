package pathXmlParser;

public class Activities {
    @XmlMultiPath(path = {"/cat/sons/son/activity:name", "/cat/child/chill/activity:name"})
    public String name;

    @XmlMultiPath(path = {"/cat/sons/son/activity:count", "/cat/child/chill/activity:count"})
    public Integer count;

    public String type;
}
