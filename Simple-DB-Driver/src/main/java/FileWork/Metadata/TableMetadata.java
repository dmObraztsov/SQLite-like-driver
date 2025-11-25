package FileWork.Metadata;

import Yadro.DataStruct.DataType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

public class TableMetadata {
    private String name;
    private int columnCount;
    private ArrayList<String> columnNames;
    //It might be necessary to create a 'ColumnsDataTypes'
    // field here so that you don't have to parse the column for data type validation every time.
    //TODO private int dataSize;

    public TableMetadata(){
    }

    public TableMetadata(String name, int columnCount)
    {
        this.name = name;
        this.columnCount = columnCount;
        columnNames = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getColumnCount() {
        return columnCount;
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
}