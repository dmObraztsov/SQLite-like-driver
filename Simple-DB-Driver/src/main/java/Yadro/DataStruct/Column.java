package Yadro.DataStruct;

import java.util.ArrayList;

public class Column {
    private final DataType type;
    private final String name;
    private final Constraints constraints;
    private final Collate collate;

    private int size;
    private final ArrayList<Object> data = new ArrayList<>();

    public Column(DataType type, String name, Constraints constraints, Collate collate) {
        this.type = type;
        this.name = name;
        this.constraints = constraints;
        this.collate = collate;
    }

    public void addToColumn(DataType content)
    {
        data.add(content);
    }

    public DataType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public Collate getCollate() {
        return collate;
    }

    public int getSize() {
        return size;
    }

    public ArrayList<Object> getData() {
        return data;
    }
}
