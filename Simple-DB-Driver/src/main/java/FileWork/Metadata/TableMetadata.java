package FileWork.Metadata;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

@Setter
@Getter
public class TableMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int columnCount;
    private int countAutoIncrements;
    private int countDefaults;
    private ArrayList<String> columnNames = new ArrayList<>();

    public TableMetadata() {
    }

    public TableMetadata(String name, int columnCount, int countAutoIncrements, int countDefaults) {
        this.name = name;
        this.columnCount = columnCount;
        this.countAutoIncrements = countAutoIncrements;
        this.countDefaults = countDefaults;
    }

    public void addColumnName(String name) {
        columnNames.add(name);
    }

    public void deleteColumnName(String name) {
        columnNames.remove(name);
    }

    public void addAutoIncrement() {
        this.countAutoIncrements++;
    }

    public void addDefault() {
        this.countDefaults++;
    }

    public void setTableName(String tableName) {

    }
}