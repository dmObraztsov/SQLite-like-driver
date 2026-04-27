package FileWork.Metadata;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class TableMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int columnCount;
    private int countAutoIncrements;
    private int countDefaults;
    private int rowSize;

    private ArrayList<ColumnMetadata> columns = new ArrayList<>();
    private ArrayList<String> columnNames = new ArrayList<>();

    public TableMetadata() {
    }

    public TableMetadata(String name, List<ColumnMetadata> columns) {
        this.name = name;
        this.columns = new ArrayList<>(columns);
        this.columnCount = columns.size();

        this.columnNames = new ArrayList<>();
        for (ColumnMetadata col : columns) {
            this.columnNames.add(col.getName());
            if (col.getDefaultValue() != null) this.countDefaults++;
        }

        this.rowSize = calculateRowSize();
    }

    public void addColumn(ColumnMetadata column) {
        this.columns.add(column);
        this.columnNames.add(column.getName());
        this.columnCount++;
        this.rowSize = calculateRowSize();
    }

    public void deleteColumn(String columnName) {
        columns.removeIf(c -> c.getName().equals(columnName));
        columnNames.remove(columnName);
        this.columnCount--;
        this.rowSize = calculateRowSize();
    }

    public void addColumnName(String name) {
        columnNames.add(name);
    }

    public void deleteColumnName(String name) {
        columnNames.remove(name);
    }

    public int calculateRowSize() {
        if (columns == null || columns.isEmpty()) {
            return 0;
        }
        return columns.stream()
                .mapToInt(ColumnMetadata::getSize)
                .sum();
    }

    public void addAutoIncrement() {
        this.countAutoIncrements++;
    }

    public void addDefault() {
        this.countDefaults++;
    }

    public void setTableName(String tableName) { }


    public ColumnMetadata getColumn(String name) {
        return columns.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}