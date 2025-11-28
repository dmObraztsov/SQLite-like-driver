package FileWork.Metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

public class TableMetadata {
    private String name;
    private int columnCount;
    private int countAutoIncrements;
    private int countDefaults;
    private ArrayList<String> columnNames;

    public TableMetadata(){
    }

    public TableMetadata(String name, int columnCount, int countAutoIncrements, int countDefaults)
    {
        this.name = name;
        this.columnCount = columnCount;
        this.countAutoIncrements = countAutoIncrements;
        this.countDefaults = countDefaults;
        columnNames = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getCountAutoIncrements() {
        return countAutoIncrements;
    }

    public int getCountDefaults() {
        return countDefaults;
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

    public void setCountAutoIncrements(int countAutoIncrements) {
        this.countAutoIncrements = countAutoIncrements;
    }

    public void setCountDefaults(int countDefaults) {
        this.countDefaults = countDefaults;
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
    public void addAutoIncrement()
    {
        this.countAutoIncrements++;
    }

    @JsonIgnore
    public void addDefault()
    {
        this.countDefaults++;
    }
}