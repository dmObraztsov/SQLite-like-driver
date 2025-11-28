package FileWork.Metadata;

import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

public class ColumnMetadata {
    private String name;
    private DataType type;
    private int size;
    private ArrayList<Constraints> constraints = new ArrayList<>();
    private Collate collate;

    public ColumnMetadata() {}

    public ColumnMetadata(String name, DataType type, int size, ArrayList<Constraints> constraints, Collate collate) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.constraints = constraints;
        this.collate = collate;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public ArrayList<Constraints> getConstraints() {
        return constraints;
    }

    public Collate getCollate() {
        return collate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setConstraints(ArrayList<Constraints> constraints) {
        this.constraints = constraints;
    }

    public void setCollate(Collate collate) {
        this.collate = collate;
    }

    @JsonIgnore
    public void incrementSize() {
        this.size++;
    }
}