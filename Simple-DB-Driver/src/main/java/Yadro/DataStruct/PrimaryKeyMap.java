package Yadro.DataStruct;


import java.util.HashMap;

public class PrimaryKeyMap {
    private HashMap<Object, Integer> links = new HashMap<>();

    public PrimaryKeyMap(){}

    public HashMap<Object, Integer> getLinks() {
        return links;
    }

    public void setLinks(HashMap<Object, Integer> links) {
        this.links = links;
    }

    public void addLink(Object key, int position)
    {
        links.put(key, position);
    }
}
