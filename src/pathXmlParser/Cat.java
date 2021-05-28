package pathXmlParser;

import java.util.Set;

public class Cat {


    @XmlPath(path = "/cat/name:first")
    public String name;

    @XmlPath(path = "/cat/surname")
    public String surname;


    //Если емеем дело со списком, то в конце указать имя элемента
    @XmlSet(setClass = Kitty.class)
    @XmlMultiPath(path = {"/cat/sons", "/cat/child"})
    public Set<Kitty> kittySet;

}
