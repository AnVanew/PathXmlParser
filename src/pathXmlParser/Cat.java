package pathXmlParser;

import java.util.Set;

public class Cat {


    @XmlPath(path = "/cat/name:first")
    private String name;

    @XmlPath(path = "/cat/surname")
    private String surname;


    //Если емеем дело со списком, то в конце указать имя элемента
    @XmlSet(setClass = Kitty.class)
    @XmlMultiPath(path = {"/cat/sons", "/cat/child"})
    @XmlSetElement(elements = {"/cat/sons/son", "/cat/child/chill"})
    private Set<Kitty> kittySet;

    public Set<Kitty> getKittySet() {
        return kittySet;
    }
}
