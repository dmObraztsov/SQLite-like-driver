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

    @JsonIgnore
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

    @JsonIgnore
    public DataType getType() {
        return type;
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

    @JsonIgnore
    public ArrayList<Constraints> getConstraints() {
        return constraints;
    }

    @JsonIgnore
    public Collate getCollate() {
        return collate;
    }

    public ArrayList<Object> getData() {
        return data;
    }

    @JsonIgnore
    public void setType(DataType type) {
        this.type = type;
    }

    @JsonIgnore
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public void setConstraints(Constraints constraint) {
        this.constraints.add(constraint);
    }

    @JsonIgnore
    public void setConstraints(ArrayList<Constraints> constraints) {
        this.constraints.addAll(constraints);
    }

    @JsonIgnore
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

    public int nullCount() {
        int count = 0;
        for(Object curr : data)
        {
            if(curr == null)
            {
                count++;
            }
        }

        return count;
    }

    //TODO
    public Object minValue() {
        return null;
    }

    //TODO
    public Object maxValue() {
        return null;
    }

    //TODO
    public int dataSize() {
        return 0;
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