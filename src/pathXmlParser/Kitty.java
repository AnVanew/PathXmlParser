package pathXmlParser;

public class Kitty {

    @XmlMultiPath(path = {"/cat/sons/son/name", "/cat/child/name"})
    public String name;

    @XmlMultiPath(path = {"/cat/sons/son/momName", "/cat/child/momName"})
    public String momName;

    public String type;
}
