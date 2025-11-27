package FileWork.Metadata;

import Yadro.DataStruct.DataType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

public class TableMetadata {
    private String name;
    private int columnCount;
    private int countPrimaryKeys;
    private ArrayList<String> columnNames;

    public TableMetadata(){
    }

    public TableMetadata(String name, int columnCount, int countPrimaryKeys)
    {
        this.name = name;
        this.columnCount = columnCount;
        this.countPrimaryKeys = countPrimaryKeys;
        columnNames = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getCountPrimaryKeys() {
        return countPrimaryKeys;
    }

    public ArrayList<String> getColumnNames() {
        return columnNames;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public void setCountPrimaryKeys(int countPrimaryKeys) {
        this.countPrimaryKeys = countPrimaryKeys;
    }

    public void setColumnNames(ArrayList<String> columnNames) {
        this.columnNames = columnNames;
    }

    @JsonIgnore
    public void addColumnName(String name)
    {
        columnNames.add(name);
    }

    @JsonIgnore
    public void deleteColumnName(String name)
    {
        columnNames.remove(name);
    }

    @JsonIgnore
    public void addPrimaryKey()
    {
        this.countPrimaryKeys++;
    }
}