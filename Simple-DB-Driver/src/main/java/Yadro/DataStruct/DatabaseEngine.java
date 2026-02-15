package Yadro.DataStruct;

import Exceptions.FileStorageException;
import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.DataType;

import java.util.*;
import java.util.stream.Collectors;

public class DatabaseEngine {
    private final FileManager fileManager;

    public DatabaseEngine(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void setCurrentDatabase(String dbName) {
        fileManager.useDB(dbName);
    }

    public void createDatabase(String dbName) throws FileStorageException {
        fileManager.createDB(dbName);
    }

    public void dropDatabase(String dbName) throws FileStorageException {
        fileManager.dropDB(dbName);
    }

    public void dropTable(String tableName) throws FileStorageException {
        fileManager.dropTableStructure(tableName);
    }

    public List<Row> select(String tableName, List<String> columns, boolean isStar, String whereCol, String whereVal) throws Exception {
        if (!fileManager.tableExists(tableName)) {
            throw new Exception("Table " + tableName + " does not exist");
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> targetColumns = isStar ? tableMeta.getColumnNames() : columns;

        Map<String, List<String>> rawData = new HashMap<>();
        for (String colName : targetColumns) {
            Column col = fileManager.loadColumnData(tableName, colName);
            rawData.put(colName, col.getData());
        }

        List<Row> rows = new ArrayList<>();
        int rowCount = rawData.values().iterator().next().size();

        for (int i = 0; i < rowCount; i++) {
            Map<String, String> rowValues = new HashMap<>();
            for (String colName : targetColumns) {
                rowValues.put(colName, rawData.get(colName).get(i));
            }

            Row row = new Row(rowValues);

            if (whereCol == null || row.get(whereCol).equals(whereVal)) {
                rows.add(row);
            }
        }

        return rows;
    }

    public List<Row> join(String table1Name, List<String> columns1, String table2Name, List<String> columns2, String leftJoinCol, String rightJoinCol) throws Exception {
        List<Row> leftTable = select(table1Name, columns1, false, null, null);
        List<Row> rightTable = select(table2Name, columns2, false, null, null);

        List<Row> joinedRows = new ArrayList<>();

        for (Row leftRow : leftTable) {
            for (Row rightRow : rightTable) {
                if (leftRow.get(leftJoinCol).equals(rightRow.get(rightJoinCol))) {

                    Map<String, String> combinedValues = new HashMap<>();
                    combinedValues.putAll(leftRow.getValuesMap());
                    combinedValues.putAll(rightRow.getValuesMap());

                    joinedRows.add(new Row(combinedValues));
                }
            }
        }

        return joinedRows;
    }

    public void insert(String tableName, List<String> columnNames, List<String> values) throws Exception {
        if (!fileManager.tableExists(tableName)) throw new Exception("Table not found");

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        Map<String, Column> columnsToUpdate = new HashMap<>();

        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);
            Column colData = fileManager.loadColumnData(tableName, colName);

            int colIndex = columnNames.indexOf(colName);
            String valueToInsert;

            if (colIndex != -1) {
                valueToInsert = values.get(colIndex);
                validateType(valueToInsert, colMeta.getType());
            } else {
                valueToInsert = "NULL";
            }

            colData.addData(colData.getData().size(), valueToInsert);
            columnsToUpdate.put(colName, colData);
        }

        for (Map.Entry<String, Column> entry : columnsToUpdate.entrySet()) {
            fileManager.saveColumnData(tableName, entry.getKey(), entry.getValue());
        }
    }

    public void createTable(String tableName, List<ColumnMetadata> columns) throws FileStorageException {
        fileManager.createTableStructure(tableName);

        TableMetadata tableMeta = new TableMetadata();
        tableMeta.setColumnCount(columns.size());

        for (ColumnMetadata colMeta : columns) {
            tableMeta.addColumnName(colMeta.getName());
            fileManager.saveColumnMetadata(tableName, colMeta.getName(), colMeta);

            fileManager.saveColumnData(tableName, colMeta.getName(), new Column());
        }

        fileManager.saveTableMetadata(tableName, tableMeta);
    }


    private void validateType(String value, DataType type) throws Exception {
        try {
            switch (type) {
                case INTEGER -> Long.parseLong(value);
                case REAL -> Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            throw new Exception("Type mismatch: expected " + type + ", got '" + value + "'");
        }
    }
}