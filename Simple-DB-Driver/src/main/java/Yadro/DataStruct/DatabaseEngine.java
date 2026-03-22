package Yadro.DataStruct;

import Exceptions.*;
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
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found");

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        Map<String, Column> columnsToUpdate = new HashMap<>();

        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);
            Column colData = loadColumn(tableName, colName);

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

        int rowCount = rawData.values().iterator().next().size();

        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereVal);
        Set<Integer> matchingSet = (matchingRows == null) ? null : new HashSet<>(matchingRows);

        List<Row> rows = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {

            if (matchingSet != null && !matchingSet.contains(i)) {
                continue;
            }

            Map<String, String> rowValues = new HashMap<>();
            for (String colName : targetColumns) {
                rowValues.put(colName, rawData.get(colName).get(i));
            }

            rows.add(new Row(rowValues));
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

    public void alterTableAddColumn(String tableName, ColumnMetadata column) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table does not exist: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        tableMeta.addColumnName(column.getName());
        tableMeta.setColumnCount(tableMeta.getColumnCount() + 1);
        fileManager.saveTableMetadata(tableName, tableMeta);

        Column newColumnData = createNewColumnWithNulls(tableName, column.getName());

        fileManager.saveColumnData(tableName, column.getName(), newColumnData);
        fileManager.saveColumnMetadata(tableName, column.getName(), column);
    }

    public void alterTableDropColumn(String tableName, String columnName) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table does not exist: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        if (!tableMeta.getColumnNames().contains(columnName)) {
            throw new NoFileException("Column does not exist: " + columnName);
        }

        tableMeta.getColumnNames().remove(columnName);
        tableMeta.setColumnCount(tableMeta.getColumnCount() - 1);

        fileManager.saveTableMetadata(tableName, tableMeta);
        fileManager.deleteColumnFiles(tableName, columnName);
    }

    public void alterTableRenameColumn(String tableName, String columnName, String newName) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table does not exist: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        if (!tableMeta.getColumnNames().contains(columnName)) {
            throw new NoFileException("Column does not exist: " + columnName);
        }

        if (tableMeta.getColumnNames().contains(newName)) {
            throw new AlreadyExistsException("Column already exists: " + newName);
        }

        int index = tableMeta.getColumnNames().indexOf(columnName);
        tableMeta.getColumnNames().set(index, newName);

        fileManager.saveTableMetadata(tableName, tableMeta);

        ColumnMetadata columnMeta = fileManager.loadColumnMetadata(tableName, columnName);
        columnMeta.setName(newName);

        fileManager.renameColumnFiles(tableName, columnName, newName);
        fileManager.saveColumnMetadata(tableName, newName, columnMeta);
    }

    public void alterTableRenameTable(String tableName, String newName) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table does not exist: " + tableName);
        }

        if (fileManager.tableExists(newName)) {
            throw new AlreadyExistsException("Table already exists: " + newName);
        }

        fileManager.renameDirectory(tableName, newName);

        TableMetadata tableMeta = fileManager.loadTableMetadata(newName);
        tableMeta.setTableName(newName);
        fileManager.saveTableMetadata(newName, tableMeta);
    }

    public int delete(String tableName, String whereCol, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table not found: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> columnNames = tableMeta.getColumnNames();

        List<Integer> matchingRows;
        if (whereCol == null) {
            int rowCount = loadColumn(tableName, columnNames.get(0)).getData().size();
            matchingRows = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                matchingRows.add(i);
            }
        } else {
            matchingRows = findMatchingRows(tableName, whereCol, whereVal);
            if (matchingRows == null || matchingRows.isEmpty()) {
                return 0;
            }
        }

        Map<String, Column> updatedColumns = new HashMap<>();

        for (String colName : columnNames) {
            Column colData = loadColumn(tableName, colName);
            List<String> data = colData.getData();

            List<String> newData = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                if (!matchingRows.contains(i)) {
                    newData.add(data.get(i));
                }
            }

            colData.setData(new ArrayList<>(newData));
            updatedColumns.put(colName, colData);
        }

        if (isTransaction) {
            transactionBuffer.computeIfAbsent(tableName, k -> new HashMap<>()).putAll(updatedColumns);
        } else {
            for (Map.Entry<String, Column> entry : updatedColumns.entrySet()) {
                fileManager.saveColumnData(tableName, entry.getKey(), entry.getValue());
            }
        }

        return matchingRows.size();
    }

    public int update(String tableName, Map<String, String> setValues, String whereCol, String whereVal) throws FileStorageException {

        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table not found: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> columnNames = tableMeta.getColumnNames();
        List<Integer> matchingRows;

        if (whereCol == null) {
            int rowCount = loadColumn(tableName, columnNames.get(0)).getData().size();
            matchingRows = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) {
                matchingRows.add(i);
            }
        } else {
            if (!columnNames.contains(whereCol)) {
                throw new NoFileException("Column does not exist: " + whereCol);
            }
            matchingRows = findMatchingRows(tableName, whereCol, whereVal);
            if (matchingRows == null || matchingRows.isEmpty()) {
                return 0;
            }
        }

        Map<String, Column> updatedColumns = new HashMap<>();
        Map<String, ColumnMetadata> metadataMap = new HashMap<>();

        for (String colName : columnNames) {
            updatedColumns.put(colName, loadColumn(tableName, colName));
            metadataMap.put(colName, fileManager.loadColumnMetadata(tableName, colName));
        }

        for (String col : setValues.keySet()) {
            if (!columnNames.contains(col)) {
                throw new NoFileException("Column does not exist: " + col);
            }
        }

        for (int rowIndex : matchingRows) {
            for (Map.Entry<String, String> entry : setValues.entrySet()) {

                String colName = entry.getKey();
                String newValue = normalizeValue(entry.getValue());

                Column column = updatedColumns.get(colName);
                ColumnMetadata colMeta = metadataMap.get(colName);

                if (!newValue.equals("NULL")) {
                    try {
                        validateType(newValue, colMeta.getType());
                    } catch (Exception e) {
                        throw new FileTypeException("Type mismatch for column " + colName + ": " + newValue);
                    }
                }

                column.getData().set(rowIndex, newValue);
            }
        }

        for (String colName : columnNames) {
            Column column = updatedColumns.get(colName);
            ColumnMetadata colMeta = metadataMap.get(colName);

            boolean isPrimaryKey = colMeta.getConstraints().contains(Constraints.PRIMARY_KEY);
            boolean isNotNull = colMeta.getConstraints().contains(Constraints.NOT_NULL);
            boolean isUnique = colMeta.getConstraints().contains(Constraints.UNIQUE);

            if (isPrimaryKey) {
                isNotNull = true;
                isUnique = true;
            }

            List<String> data = column.getData();
            Set<String> uniqueCheck = new HashSet<>();

            for (String value : data) {
                if (isNotNull && value.equals("NULL")) {
                    throw new FileStorageException("Column '" + colName + "' cannot be NULL");
                }

                if (isUnique && !value.equals("NULL")) {
                    if (!uniqueCheck.add(value)) {
                        throw new AlreadyExistsException(
                                "Duplicate value '" + value + "' in column '" + colName + "'"
                        );
                    }
                }

                if (colMeta.getConstraints().contains(Constraints.CHECK) && colMeta.getCheckExpression() != null && !value.equals("NULL") && !evaluateSimpleCheck(colMeta.getCheckExpression(), colName, value)) {
                    throw new FileStorageException(
                            "CHECK constraint failed for column '" + colName + "'"
                    );
                }
            }
        }

        if (isTransaction) {
            transactionBuffer
                    .computeIfAbsent(tableName, k -> new HashMap<>())
                    .putAll(updatedColumns);
        } else {
            for (Map.Entry<String, Column> entry : updatedColumns.entrySet()) {
                fileManager.saveColumnData(tableName, entry.getKey(), entry.getValue());
            }
        }

        return matchingRows.size();
    }

    private List<Integer> findMatchingRows(String tableName, String whereCol, String whereVal) throws FileStorageException {
        if (whereCol == null) {
            return null;
        }

        Column column = loadColumn(tableName, whereCol);
        List<String> data = column.getData();

        String normalizedWhereVal = normalizeValue(whereVal);

        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            String value = data.get(i);
            if (value != null && value.equals(normalizedWhereVal)) {
                result.add(i);
            }
        }

        return result;
    }

    private String normalizeValue(String value) {
        if (value == null) return null;

        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }

        return value;
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

    private Column createNewColumnWithNulls(String tableName, String columnName) throws FileStorageException {
        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        int rowCount = 0;

        if (!tableMeta.getColumnNames().isEmpty()) {
            rowCount = loadColumn(tableName, tableMeta.getColumnNames().get(0)).getData().size();
        }

        Column newColumn = new Column();
        for (int i = 0; i < rowCount; i++) {
            newColumn.addData(i, "NULL");
        }

        return newColumn;
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
}