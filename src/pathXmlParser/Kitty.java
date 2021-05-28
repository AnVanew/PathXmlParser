package pathXmlParser;

import java.util.Set;

public class Kitty {

    @XmlMultiPath(path = {"/cat/sons/son:name", "/cat/child/name"})
    public String name;

    @XmlMultiPath(path = {"/cat/sons/son:momName", "/cat/child/momName"})
    public String momName;

    public String type;

    @XmlInnerClass(path = "/cat/sons/son/activity")
    public Activities activity;
}
