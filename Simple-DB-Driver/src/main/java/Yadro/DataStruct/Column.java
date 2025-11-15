package Yadro.DataStruct;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Column {
    private DataType type;
    private String name;
    private final ArrayList<Constraints> constraints = new ArrayList<>();
    private Collate collate;

    private final ArrayList<Object> data = new ArrayList<>();

    public Column() {}

    public Column(DataType type, String name, ArrayList<Constraints> constraints, Collate collate) {
        this.type = type;
        this.name = name;
        if(!constraints.isEmpty()) this.constraints.addAll(constraints);
        if(collate != null) this.collate = collate;
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

    public ArrayList<Constraints> getConstraints() {
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

//    public void setConstraints(Constraints constraint) {
//        this.constraints.add(constraint);
//    }

    public void setConstraints(ArrayList<Constraints> constraints) {
        this.constraints.addAll(constraints);
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