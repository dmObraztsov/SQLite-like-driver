package Yadro.DataStruct;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Column {
    private DataType type;
    private String name;
    private Constraints constraints;
    private Collate collate;

    private final ArrayList<Object> data = new ArrayList<>();

    //TODO Constructor handles NULL fields

    public Column() {}

    public Column(DataType type, String name, Constraints constraints, Collate collate) {
        this.type = type;
        this.name = name;
        this.constraints = constraints;
        this.collate = collate;
    }

    public Column(DataType type, String name, Constraints constraints) {
        this.type = type;
        this.name = name;
        this.constraints = constraints;
    }

    public Column(DataType type, String name, Collate collate) {
        this.type = type;
        this.name = name;
        this.collate = collate;
    }

    public Column(DataType type, String name) {
        this.type = type;
        this.name = name;
    }

    public void addToColumn(Object content) {
        data.add(content);
    }

    @JsonIgnore
    public int getSize() {
        return data.size();
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

    public ArrayList<Object> getData() {
        return data;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public void setCollate(Collate collate) {
        this.collate = collate;
    }

    public void setData(ArrayList<Object> data) {
        this.data.clear();
        if (data != null) {
            this.data.addAll(data);
        }
    }

    public Object getValueAt(int index) {
        if (index >= 0 && index < data.size()) {
            return data.get(index);
        }
        return null;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public String toString() {
        return "Column{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", size=" + data.size() +
                '}';
    }
}