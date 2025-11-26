package FileWork.Metadata;

import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

import java.util.ArrayList;

public class ColumnMetadata {
    private String name;
    private DataType type;
    private int size;
    private ArrayList<Constraints> constraints = new ArrayList<>();
    private Collate collate;
    private int nullCount;
    private Object minValue;
    private Object maxValue;
    // TODO private long dataSize;

    public ColumnMetadata(String testColumnName, String string, boolean b, boolean b1) {}

    public ColumnMetadata(String name, DataType type, int size, ArrayList<Constraints> constraints, Collate collate, int nullCount, Object minValue, Object maxValue) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.constraints = constraints;
        this.collate = collate;
        this.nullCount = nullCount;
        this.minValue = minValue;
        this.maxValue = maxValue;
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

    public int getNullCount() {
        return nullCount;
    }

    public Object getMinValue() {
        return minValue;
    }

    public Object getMaxValue() {
        return maxValue;
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

    public void setNullCount(int nullCount) {
        this.nullCount = nullCount;
    }

    public void setMinValue(Object minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(Object maxValue) {
        this.maxValue = maxValue;
    }
}