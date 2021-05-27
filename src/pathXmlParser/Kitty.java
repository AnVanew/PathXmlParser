package pathXmlParser;

public class Kitty {

    @XmlMultiPath(path = {"/cat/sons/name", "/cat/child/name"})
    public String name;

    @XmlMultiPath(path = {"/cat/sons/momName", "/cat/child/momName"})
    public String momName;

    int Type;
}
