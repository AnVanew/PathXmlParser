package pathXmlParser;

import java.util.Set;

public class Kitty {

    @XmlMultiPath(path = {"/cat/sons/son:name", "/cat/child/chill:name"})
    public String name;

    @XmlMultiPath(path = {"/cat/sons/son:momName", "/cat/child/chill:momName"})
    public String momName;

    public String type;

    @XmlInnerClass(path = {"/cat/sons/son/activity", "/cat/child/chill/activity"})
    public Activities activity;
}
