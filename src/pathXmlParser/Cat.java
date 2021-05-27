package pathXmlParser;

import java.util.Set;

public class Cat {


    @XmlPath(path = "/cat/name:first")
    public String name;

    @XmlPath(path = "/cat/surname")
    public String surname;

    @XmlSet(setClass = Kitty.class)
    @XmlMultiPath(path = {"/cat/sons", "/cat/child"})
    public Set<Kitty> kittySet;

}
