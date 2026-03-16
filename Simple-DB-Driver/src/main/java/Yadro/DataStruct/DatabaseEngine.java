package Yadro.DataStruct;

import Exceptions.FileStorageException;
import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;

import java.util.*;

public class DatabaseEngine {
    private final FileManager fileManager;
    private boolean isTransaction;

    private final Map<String, Map<String, Column>> transactionBuffer;

    public DatabaseEngine(FileManager fileManager) {
        this.fileManager = fileManager;
        this.isTransaction = false;
        this.transactionBuffer = new HashMap<>();
    }

    public void setCurrentDatabase(String dbName) throws FileStorageException {
        fileManager.useDB(dbName);
    }

    public void createDatabase(String dbName) throws FileStorageException {
        fileManager.createDB(dbName);
    }

    public void dropDatabase(String dbName) throws FileStorageException {
        fileManager.dropDB(dbName);
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

    public void dropTable(String tableName) throws FileStorageException {
        fileManager.dropTableStructure(tableName);
    }

    public void insert(String tableName, List<String> columnNames, List<String> values) throws Exception {
        if (!fileManager.tableExists(tableName)) throw new Exception("Table not found");

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        Map<String, Column> columnsToUpdate = new HashMap<>();
        Map<String, String> finalValues = new HashMap<>();

        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);
            Column colData = loadColumn(tableName, colName);

            int colIndex = columnNames.indexOf(colName);
            String valueToInsert;

            if (colIndex != -1) {
                valueToInsert = values.get(colIndex);
            } else if (colMeta.getDefaultValue() != null) {
                valueToInsert = colMeta.getDefaultValue();
            } else {
                valueToInsert = "NULL";
            }

            if (colMeta.getConstraints().contains(Constraints.AUTOINCREMENT) && valueToInsert.equals("NULL")) {
                int nextValue = colData.getData().size() + 1;
                valueToInsert = String.valueOf(nextValue);
            }

            if (!valueToInsert.equals("NULL")) {
                validateType(valueToInsert, colMeta.getType());
            }

            finalValues.put(colName, valueToInsert);
            columnsToUpdate.put(colName, colData);
        }

        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);
            Column colData = loadColumn(tableName, colName);
            String value = finalValues.get(colName);

            boolean isPrimaryKey = colMeta.getConstraints().contains(Constraints.PRIMARY_KEY);
            boolean isNotNull = colMeta.getConstraints().contains(Constraints.NOT_NULL);
            boolean isUnique = colMeta.getConstraints().contains(Constraints.UNIQUE);

            if (isPrimaryKey) {
                isNotNull = true;
                isUnique = true;
            }

            if (isNotNull && value.equals("NULL")) {
                throw new Exception("Column " + colName + " can not be NULL");
            }

            if (isUnique && !value.equals("NULL") && colData.getData().contains(value)) {
                throw new Exception("Column " + colName + " is UNIQUE and it already contains " + value);
            }

            if (colMeta.getConstraints().contains(Constraints.CHECK)
                    && colMeta.getCheckExpression() != null
                    && !value.equals("NULL")
                    && !evaluateSimpleCheck(colMeta.getCheckExpression(), colName, value)) {
                throw new Exception("CHECK constraint failed for column " + colName + ": " + colMeta.getCheckExpression());
            }
        }

        for (String colName : tableMeta.getColumnNames()) {
            Column colData = columnsToUpdate.get(colName);
            String value = finalValues.get(colName);
            colData.addData(colData.getData().size(), value);
        }

        if (isTransaction) {
            transactionBuffer.computeIfAbsent(tableName, k -> new HashMap<>()).putAll(columnsToUpdate);
        } else {
            for (Map.Entry<String, Column> entry : columnsToUpdate.entrySet()) {
                fileManager.saveColumnData(tableName, entry.getKey(), entry.getValue());
            }
        }
    }

    public List<Row> select(String tableName, List<String> columns, boolean isStar, String whereCol, String whereVal) throws Exception {
        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> targetColumns = isStar ? tableMeta.getColumnNames() : columns;

        Map<String, List<String>> rawData = new HashMap<>();
        for (String colName : targetColumns) {
            Column col = loadColumn(tableName, colName);
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
        List<Row> leftTable = select(table1Name, null, true, null, null);
        List<Row> rightTable = select(table2Name, null, true, null, null);

        List<Row> joinedRows = new ArrayList<>();

        for (Row leftRow : leftTable) {
            for (Row rightRow : rightTable) {
                String left = leftRow.get(leftJoinCol);
                String right = rightRow.get(rightJoinCol);
                if ((left == null) || (right == null)) {
                    continue;
                }

                if (left.equals(right)) {
                    Map<String, String> combinedValues = new HashMap<>();

                    Map<String, String> leftValues = (columns1 != null)
                            ? leftRow.getValuesMap(columns1)
                            : leftRow.getValuesMap();
                    Map<String, String> rightValues = (columns2 != null)
                            ? rightRow.getValuesMap(columns2)
                            : rightRow.getValuesMap();

                    for (Map.Entry<String, String> entry : leftValues.entrySet()) {
                        combinedValues.put(table1Name + "." + entry.getKey(), entry.getValue());
                    }

                    for (Map.Entry<String, String> entry : rightValues.entrySet()) {
                        combinedValues.put(table2Name + "." + entry.getKey(), entry.getValue());
                    }

                    joinedRows.add(new Row(combinedValues));
                }
            }
        }

        return joinedRows;
    }



    public void beginTransaction() {
        if (isTransaction) {
            throw new IllegalStateException("Transaction already active");
        }
        isTransaction = true;
        transactionBuffer.clear();
    }

    public void commit() throws FileStorageException {
        if (!isTransaction) {
            throw new IllegalStateException("No active transaction");
        }

        Map<String, Map<String, Column>> snapshot = new HashMap<>();
        for (Map.Entry<String, Map<String, Column>> tableEntry : transactionBuffer.entrySet()) {
            String tableName = tableEntry.getKey();
            Map<String, Column> tableSnapshot = new HashMap<>();
            for (String colName : tableEntry.getValue().keySet()) {
                tableSnapshot.put(colName, fileManager.loadColumnData(tableName, colName));
            }
            snapshot.put(tableName, tableSnapshot);
        }

        try {
            for (Map.Entry<String, Map<String, Column>> tableEntry : transactionBuffer.entrySet()) {
                String tableName = tableEntry.getKey();
                for (Map.Entry<String, Column> colEntry : tableEntry.getValue().entrySet()) {
                    fileManager.saveColumnData(tableName, colEntry.getKey(), colEntry.getValue());
                }
            }
        } catch (FileStorageException e) {
            restoreSnapshot(snapshot);
            throw new FileStorageException("Commit failed, disk state restored. " +
                    "You can retry commit() or call rollback() to cancel. Cause: " + e.getMessage());
        }

        transactionBuffer.clear();
        isTransaction = false;
    }

    public void rollback() {
        if (!isTransaction) {
            throw new IllegalStateException("No active transaction");
        }
        transactionBuffer.clear();
        isTransaction = false;
    }

    private void restoreSnapshot(Map<String, Map<String, Column>> snapshot) {
        for (Map.Entry<String, Map<String, Column>> tableEntry : snapshot.entrySet()) {
            String tableName = tableEntry.getKey();
            for (Map.Entry<String, Column> colEntry : tableEntry.getValue().entrySet()) {
                try {
                    fileManager.saveColumnData(tableName, colEntry.getKey(), colEntry.getValue());
                } catch (FileStorageException ignored) {
                    //TODO здесь фатальная ошибка, если мы делаем без журналирования, надо подумать че делать
                }
            }
        }
    }

    public boolean isTransactionActive() {
        return isTransaction;
    }

    private Column loadColumn(String tableName, String colName) throws FileStorageException {
        if (isTransaction && transactionBuffer.containsKey(tableName)) {
            Map<String, Column> tableBuffer = transactionBuffer.get(tableName);
            if (tableBuffer.containsKey(colName)) {
                Column buffered = tableBuffer.get(colName);
                Column copy = new Column();
                copy.setData(new ArrayList<>(buffered.getData()));
                return copy;
            }
        }
        return fileManager.loadColumnData(tableName, colName);
    }

    private boolean evaluateSimpleCheck(String expression, String columnName, String value) {
        String normalized = expression.replaceAll("\\s+", "");
        String[] operators = {">=", "<=", "!=", "<>", "=", ">", "<"};

        for (String operator : operators) {
            int idx = normalized.indexOf(operator);
            if (idx <= 0) {
                continue;
            }

            String left = normalized.substring(0, idx);
            String right = normalized.substring(idx + operator.length());
            if (!left.equals(columnName)) {
                continue;
            }

            return compareCheckValues(value, right, operator);
        }

        return true;
    }

    private boolean compareCheckValues(String value, String rightOperand, String operator) {
        boolean numericCompare = isNumeric(value) && isNumeric(rightOperand);

        int cmp;
        if (numericCompare) {
            double leftNum = Double.parseDouble(value);
            double rightNum = Double.parseDouble(rightOperand);
            cmp = Double.compare(leftNum, rightNum);
        } else {
            cmp = value.compareTo(rightOperand);
        }

        return switch (operator) {
            case "=" -> cmp == 0;
            case "!=", "<>" -> cmp != 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> true;
        };
    }

    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void validateType(String value, DataType type) throws Exception {
        try {
            switch (type) {
                case INTEGER -> Long.parseLong(value);
                case REAL -> Double.parseDouble(value);
                case TEXT, BLOB, NULL -> {
                }
            }
        } catch (NumberFormatException e) {
            throw new Exception("Type mismatch: expected " + type + ", got '" + value + "'");
        }
    }

    public void alterTableAddColumn(String tableName, ColumnMetadata column) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new IllegalArgumentException("Table does not exist: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        tableMeta.addColumnName(column.getName());
        tableMeta.setColumnCount(tableMeta.getColumnCount() + 1);

        fileManager.saveTableMetadata(tableName, tableMeta);

        Column newColumnData = new Column();

        fileManager.saveColumnData(tableName, column.getName(), newColumnData);
        fileManager.saveColumnMetadata(tableName, column.getName(), column);
    }

    public void alterTableDropColumn(String tableName, String columnName) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new IllegalArgumentException("Table does not exist: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        if (!tableMeta.getColumnNames().contains(columnName)) {
            throw new IllegalArgumentException("Column does not exist: " + columnName);
        }

        tableMeta.getColumnNames().remove(columnName);
        tableMeta.setColumnCount(tableMeta.getColumnCount() - 1);

        fileManager.saveTableMetadata(tableName, tableMeta);
        fileManager.deleteColumnFiles(tableName, columnName);
    }
}