package FileWork.Metadata;

import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Column;
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
    private int nullCount;
    private Object minValue;
    private Object maxValue;
    // TODO private long dataSize;

    public ColumnMetadata() {
    }

    public ColumnMetadata(Column column)
    {
        this.name = column.getName();
        this.type = column.getType();
        this.size = column.getSize();
        this.constraints = column.getConstraints();
        this.collate = column.getCollate();
        this.nullCount = column.nullCount();
        this.minValue = column.minValue();
        this.maxValue = column.maxValue();
        //this.dataSize = column.dataSize();
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