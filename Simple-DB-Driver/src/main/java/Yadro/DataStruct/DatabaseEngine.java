package Yadro.DataStruct;

import Exceptions.FileTypeException;
import Exceptions.AlreadyExistsException;
import Exceptions.FileStorageException;
import Exceptions.NoFileException;
import Exceptions.NoTableException;
import FileWork.FileManager;
import FileWork.Index.ColumnIndex;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import FileWork.WAL.WalEntry;

import java.util.*;

public class DatabaseEngine {
    private final FileManager fileManager;
    private boolean isTransaction;
    private final Map<String, Map<String, Column>> transactionBuffer;
    private String currentTxId;

    public DatabaseEngine(FileManager fileManager) {
        this.fileManager = fileManager;
        this.isTransaction = false;
        this.transactionBuffer = new HashMap<>();
    }

    public void setCurrentDatabase(String dbName) throws FileStorageException {
        fileManager.useDB(dbName);
        recoverIfNeeded();
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

            if (colMeta.getConstraints().contains(Constraints.PRIMARY_KEY)
                    || colMeta.getConstraints().contains(Constraints.UNIQUE)) {
                createEmptyIndex(tableName, colMeta.getName());
            }
        }

        fileManager.saveTableMetadata(tableName, tableMeta);
    }

    public void dropTable(String tableName) throws FileStorageException {
        fileManager.dropTableStructure(tableName);
    }

    public void insert(String tableName, List<String> columnNames, List<String> values) throws Exception {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found");

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> allTableColumns = tableMeta.getColumnNames();

        if (columnNames == null || columnNames.isEmpty()) {
            if (values.size() != allTableColumns.size()) {
                throw new Exception("Column count mismatch");
            }
            columnNames = allTableColumns;
        }

        Map<String, Column> columnsToUpdate = new HashMap<>();
        Map<String, String> finalValues = new HashMap<>();
        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);
            Column colData = loadColumn(tableName, colName);

            int colIndex = -1;
            for (int j = 0; j < columnNames.size(); j++) {
                if (columnNames.get(j).equalsIgnoreCase(colName)) {
                    colIndex = j;
                    break;
                }
            }
            String valueToInsert;
            if (colIndex != -1) {
                valueToInsert = values.get(colIndex);
            } else if (colMeta.getDefaultValue() != null) {
                valueToInsert = colMeta.getDefaultValue();
            } else {
                valueToInsert = "NULL";
            }

            if (colMeta.getConstraints().contains(Constraints.AUTOINCREMENT) && valueToInsert.equals("NULL")) {
                int nextValue = colData.getData().stream()
                        .filter(v -> v != null && !v.equals("NULL"))
                        .mapToInt(v -> { try { return Integer.parseInt(v); } catch (NumberFormatException e) { return 0; } })
                        .max()
                        .orElse(0) + 1;
                valueToInsert = String.valueOf(nextValue);
            }

            if (!valueToInsert.equals("NULL")) {
                validateType(valueToInsert, colMeta.getType());
            }

            finalValues.put(colName, normalizeValue(valueToInsert));
            columnsToUpdate.put(colName, colData);
        }

        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);
            Column colData = columnsToUpdate.get(colName);
            String value = finalValues.get(colName);

            boolean isPrimaryKey = colMeta.getConstraints().contains(Constraints.PRIMARY_KEY);
            boolean isNotNull    = colMeta.getConstraints().contains(Constraints.NOT_NULL);
            boolean isUnique     = colMeta.getConstraints().contains(Constraints.UNIQUE);

            if (isPrimaryKey) { isNotNull = true; isUnique = true; }

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
                throw new Exception("CHECK constraint failed for column " + colName
                        + ": " + colMeta.getCheckExpression());
            }
        }

        int newRowIndex = columnsToUpdate.values().iterator().next().getData().size();
        for (String colName : tableMeta.getColumnNames()) {
            Column colData = columnsToUpdate.get(colName);
            String value   = finalValues.get(colName);
            colData.addData(colData.getData().size(), value);
        }

        if (isTransaction) {
            transactionBuffer
                    .computeIfAbsent(tableName, k -> new HashMap<>())
                    .putAll(columnsToUpdate);
        } else {
            for (Map.Entry<String, Column> entry : columnsToUpdate.entrySet()) {
                fileManager.saveColumnData(tableName, entry.getKey(), entry.getValue());
                ColumnMetadata meta = fileManager.loadColumnMetadata(tableName, entry.getKey());
                meta.setSize(entry.getValue().getData().size());
                fileManager.saveColumnMetadata(tableName, entry.getKey(), meta);
            }
            updateIndexesAfterInsert(tableName, finalValues, newRowIndex);
        }
    }

    public List<Row> select(String tableName, List<String> columns, boolean isStar,
                            String whereCol, String whereOp, String whereVal, boolean isDistinct) throws Exception {
        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> targetColumns = isStar ? tableMeta.getColumnNames() : columns;

        Map<String, List<String>> rawData = new HashMap<>();
        for (String colName : targetColumns) {
            Column col = loadColumn(tableName, colName);
            rawData.put(colName, col.getData());
        }

        if (rawData.isEmpty()) return Collections.emptyList();
        int rowCount = rawData.values().iterator().next().size();

        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
        Set<Integer> matchingSet = (matchingRows == null) ? null : new HashSet<>(matchingRows);

        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            if (matchingSet != null && !matchingSet.contains(i)) continue;
            Map<String, String> rowValues = new HashMap<>();
            for (String colName : targetColumns) {
                rowValues.put(colName, rawData.get(colName).get(i));
            }
            rows.add(new Row(rowValues));
        }

        if (isDistinct) {
            return new ArrayList<>(new LinkedHashSet<>(rows));
        }
        return rows;
    }

    public List<Row> join(String table1Name, List<String> columns1,
                          String table2Name, List<String> columns2,
                          String leftJoinCol, String rightJoinCol) throws Exception {
        List<Row> leftTable  = select(table1Name, null, true, null, null, null, false);
        List<Row> rightTable = select(table2Name, null, true, null, null, null, false);

        List<Row> joinedRows = new ArrayList<>();
        for (Row leftRow : leftTable) {
            for (Row rightRow : rightTable) {
                String left  = leftRow.get(leftJoinCol);
                String right = rightRow.get(rightJoinCol);
                if (left == null || right == null) continue;
                if (!left.equals(right)) continue;

                Map<String, String> combinedValues = new HashMap<>();
                Map<String, String> leftValues  = (columns1 != null) ? leftRow.getValuesMap(columns1)  : leftRow.getValuesMap();
                Map<String, String> rightValues = (columns2 != null) ? rightRow.getValuesMap(columns2) : rightRow.getValuesMap();

                leftValues .forEach((k, v) -> combinedValues.put(table1Name + "." + k, v));
                rightValues.forEach((k, v) -> combinedValues.put(table2Name + "." + k, v));
                joinedRows.add(new Row(combinedValues));
            }
        }
        return joinedRows;
    }

    public int delete(String tableName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table not found: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> columnNames = tableMeta.getColumnNames();

        List<Integer> matchingRows;
        if (whereCol == null) {
            int rowCount = loadColumn(tableName, columnNames.get(0)).getData().size();
            matchingRows = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) matchingRows.add(i);
        } else {
            matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
            if (matchingRows == null || matchingRows.isEmpty()) return 0;
        }

        Map<String, Column> updatedColumns = new HashMap<>();
        Set<Integer> toDelete = new HashSet<>(matchingRows);
        for (String colName : columnNames) {
            Column colData = loadColumn(tableName, colName);
            List<String> newData = new ArrayList<>();
            for (int i = 0; i < colData.getData().size(); i++) {
                if (!toDelete.contains(i)) newData.add(colData.getData().get(i));
            }
            colData.setData(new ArrayList<>(newData));
            updatedColumns.put(colName, colData);
        }

        if (isTransaction) {
            transactionBuffer
                    .computeIfAbsent(tableName, k -> new HashMap<>())
                    .putAll(updatedColumns);
        } else {
            for (Map.Entry<String, Column> entry : updatedColumns.entrySet()) {
                fileManager.saveColumnData(tableName, entry.getKey(), entry.getValue());
            }

            rebuildIndexesForTable(tableName);
        }

        return matchingRows.size();
    }

    public int update(String tableName, Map<String, String> setValues,
                      String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) {
            throw new NoTableException("Table not found: " + tableName);
        }

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        List<String> columnNames = tableMeta.getColumnNames();

        List<Integer> matchingRows;
        if (whereCol == null) {
            int rowCount = loadColumn(tableName, columnNames.get(0)).getData().size();
            matchingRows = new ArrayList<>();
            for (int i = 0; i < rowCount; i++) matchingRows.add(i);
        } else {
            if (!columnNames.contains(whereCol)) throw new NoFileException("Column does not exist: " + whereCol);
            matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
            if (matchingRows == null || matchingRows.isEmpty()) return 0;
        }

        Map<String, Column>         updatedColumns = new HashMap<>();
        Map<String, ColumnMetadata> metadataMap    = new HashMap<>();
        for (String colName : columnNames) {
            updatedColumns.put(colName, loadColumn(tableName, colName));
            metadataMap.put(colName, fileManager.loadColumnMetadata(tableName, colName));
        }

        for (String col : setValues.keySet()) {
            if (!columnNames.contains(col)) throw new NoFileException("Column does not exist: " + col);
        }

        for (int rowIndex : matchingRows) {
            for (Map.Entry<String, String> entry : setValues.entrySet()) {
                String colName  = entry.getKey();
                String newValue = normalizeValue(entry.getValue());
                Column column   = updatedColumns.get(colName);
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
            boolean isNotNull    = colMeta.getConstraints().contains(Constraints.NOT_NULL);
            boolean isUnique     = colMeta.getConstraints().contains(Constraints.UNIQUE);
            if (isPrimaryKey) { isNotNull = true; isUnique = true; }

            List<String> data = column.getData();
            Set<String> uniqueCheck = new HashSet<>();
            for (String value : data) {
                if (isNotNull && value.equals("NULL"))
                    throw new FileStorageException("Column '" + colName + "' cannot be NULL");
                if (isUnique && !value.equals("NULL") && !uniqueCheck.add(value))
                    throw new AlreadyExistsException("Duplicate value '" + value + "' in column '" + colName + "'");
                if (colMeta.getConstraints().contains(Constraints.CHECK)
                        && colMeta.getCheckExpression() != null
                        && !value.equals("NULL")
                        && !evaluateSimpleCheck(colMeta.getCheckExpression(), colName, value))
                    throw new FileStorageException("CHECK constraint failed for column '" + colName + "'");
            }
        }

        if (isTransaction) {
            transactionBuffer
                    .computeIfAbsent(tableName, k -> new HashMap<>())
                    .putAll(updatedColumns);
        } else {
            // In-place: write only the cells that were actually modified
            for (int rowIndex : matchingRows) {
                for (Map.Entry<String, String> entry : setValues.entrySet()) {
                    String colName = entry.getKey();
                    String newVal  = normalizeValue(entry.getValue());
                    fileManager.writeColumnRow(tableName, colName, rowIndex, newVal);
                }
            }
            rebuildIndexesForTable(tableName);
        }

        return matchingRows.size();
    }


    public void beginTransaction() {
        if (isTransaction) throw new IllegalStateException("Transaction already active");
        isTransaction = true;
        currentTxId   = UUID.randomUUID().toString();
        transactionBuffer.clear();
    }

    public void commit() throws FileStorageException {
        if (!isTransaction) throw new IllegalStateException("No active transaction");

        try {
            fileManager.ensureWalDirExists();

            List<WalEntry.WalColumnEntry> walColumns = new ArrayList<>();
            for (Map.Entry<String, Map<String, Column>> tableEntry : transactionBuffer.entrySet()) {
                String tableName = tableEntry.getKey();
                for (String colName : tableEntry.getValue().keySet()) {
                    List<String> originalData;
                    try {
                        originalData = new ArrayList<>(
                                fileManager.loadColumnData(tableName, colName).getData());
                    } catch (FileStorageException e) {
                        originalData = Collections.emptyList();
                    }
                    walColumns.add(new WalEntry.WalColumnEntry(tableName, colName, originalData));
                }
            }

            WalEntry walEntry = new WalEntry(currentTxId, "PENDING", walColumns);
            fileManager.writeWalAtomic(currentTxId, walEntry);

            for (Map.Entry<String, Map<String, Column>> tableEntry : transactionBuffer.entrySet()) {
                String tableName = tableEntry.getKey();
                for (Map.Entry<String, Column> colEntry : tableEntry.getValue().entrySet()) {
                    ArrayList<String> dataCopy = new ArrayList<>(colEntry.getValue().getData());
                    fileManager.saveColumnData(tableName, colEntry.getKey(), new Column(dataCopy));
                    ColumnMetadata meta = fileManager.loadColumnMetadata(tableName, colEntry.getKey());
                    meta.setSize(dataCopy.size());
                    fileManager.saveColumnMetadata(tableName, colEntry.getKey(), meta);
                }

                rebuildIndexesForTable(tableName);
            }

            fileManager.deleteWal(currentTxId);

        } catch (FileStorageException e) {
            transactionBuffer.clear();
            isTransaction = false;
            currentTxId   = null;
            throw new FileStorageException(
                    "Commit failed. Database will be recovered automatically on next startup. Cause: " + e.getMessage());
        }

        transactionBuffer.clear();
        isTransaction = false;
        currentTxId   = null;
    }

    public void rollback() {
        if (!isTransaction) throw new IllegalStateException("No active transaction");
        transactionBuffer.clear();
        isTransaction = false;
        currentTxId   = null;
    }

    public boolean isTransactionActive() {
        return isTransaction;
    }

    public void recoverIfNeeded() {
        for (String txId : fileManager.listWalTmpIds()) {
            try {
                fileManager.deleteWalTmp(txId);
                System.out.println("[WAL] Removed incomplete WAL tmp: " + txId);
            } catch (FileStorageException ignored) {}
        }

        for (String txId : fileManager.listPendingWalIds()) {
            try {
                WalEntry wal = fileManager.loadWal(txId);
                System.out.println("[WAL] Recovering transaction: " + txId);

                Set<String> affectedTables = new HashSet<>();

                for (WalEntry.WalColumnEntry entry : wal.getColumns()) {
                    Column original = new Column(new ArrayList<>(entry.getOriginalData()));
                    fileManager.saveColumnData(entry.getTableName(), entry.getColumnName(), original);
                    affectedTables.add(entry.getTableName());
                }

                for (String tableName : affectedTables) {
                    rebuildIndexesForTable(tableName);
                }

                fileManager.deleteWal(txId);
                System.out.println("[WAL] Recovery complete for transaction: " + txId);
            } catch (FileStorageException e) {
                System.err.println("[WAL] WARNING: Could not recover transaction " + txId + ": " + e.getMessage());
            }
        }
    }

    public void alterTableAddColumn(String tableName, ColumnMetadata column) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table does not exist: " + tableName);

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        tableMeta.addColumnName(column.getName());
        tableMeta.setColumnCount(tableMeta.getColumnCount() + 1);
        fileManager.saveTableMetadata(tableName, tableMeta);

        Column newColumnData = createNewColumnWithNulls(tableName);
        fileManager.saveColumnData(tableName, column.getName(), newColumnData);
        fileManager.saveColumnMetadata(tableName, column.getName(), column);

        if (column.getConstraints().contains(Constraints.PRIMARY_KEY)
                || column.getConstraints().contains(Constraints.UNIQUE)) {
            rebuildIndexForColumn(tableName, column.getName());
        }
    }

    public void alterTableDropColumn(String tableName, String columnName) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table does not exist: " + tableName);

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        if (!tableMeta.getColumnNames().contains(columnName)) throw new NoFileException("Column does not exist: " + columnName);

        tableMeta.getColumnNames().remove(columnName);
        tableMeta.setColumnCount(tableMeta.getColumnCount() - 1);
        fileManager.saveTableMetadata(tableName, tableMeta);
        fileManager.deleteColumnFiles(tableName, columnName);

        if (fileManager.indexExists(tableName, columnName)) {
            fileManager.deleteIndex(tableName, columnName);
        }
    }

    public void alterTableRenameColumn(String tableName, String columnName, String newName) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table does not exist: " + tableName);

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        if (!tableMeta.getColumnNames().contains(columnName))    throw new NoFileException("Column does not exist: " + columnName);
        if (tableMeta.getColumnNames().contains(newName))         throw new AlreadyExistsException("Column already exists: " + newName);

        int index = tableMeta.getColumnNames().indexOf(columnName);
        tableMeta.getColumnNames().set(index, newName);
        fileManager.saveTableMetadata(tableName, tableMeta);

        ColumnMetadata columnMeta = fileManager.loadColumnMetadata(tableName, columnName);
        columnMeta.setName(newName);
        fileManager.renameColumnFiles(tableName, columnName, newName);
        fileManager.saveColumnMetadata(tableName, newName, columnMeta);

        if (fileManager.indexExists(tableName, columnName)) {
            ColumnIndex idx = fileManager.loadIndex(tableName, columnName);
            idx.setColumnName(newName);
            fileManager.saveIndex(tableName, newName, idx);
            fileManager.deleteIndex(tableName, columnName);
        }
    }

    public void alterTableRenameTable(String tableName, String newName) throws FileStorageException {
        if (!fileManager.tableExists(tableName))  throw new NoTableException("Table does not exist: " + tableName);
        if (fileManager.tableExists(newName))      throw new AlreadyExistsException("Table already exists: " + newName);

        fileManager.renameDirectory(tableName, newName);
        TableMetadata tableMeta = fileManager.loadTableMetadata(newName);
        tableMeta.setTableName(newName);
        fileManager.saveTableMetadata(newName, tableMeta);
    }

    public int count(String tableName, String columnName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found: " + tableName);

        List<String> columnNames = fileManager.loadTableMetadata(tableName).getColumnNames();
        String targetColumn = (columnName == null || columnName.equals("*")) ? columnNames.get(0) : columnName;

        Column column = loadColumn(tableName, targetColumn);
        if (whereCol == null) return column.getData().size();

        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
        return matchingRows == null ? 0 : matchingRows.size();
    }

    public double sum(String tableName, String columnName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found: " + tableName);

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        String targetColumn = (columnName == null || columnName.equals("*")) ? tableMeta.getColumnNames().get(0) : columnName;

        ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, targetColumn);
        DataType type = colMeta.getType();
        if (type != DataType.INTEGER && type != DataType.REAL)
            throw new FileTypeException("Cannot SUM non-numeric column: " + targetColumn);

        Column column = loadColumn(tableName, targetColumn);
        List<String> data = column.getData();
        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
        Set<Integer> matchingSet = (matchingRows == null) ? null : new HashSet<>(matchingRows);

        double sum = 0;
        for (int i = 0; i < data.size(); i++) {
            if (matchingSet != null && !matchingSet.contains(i)) continue;
            String value = data.get(i);
            if (value == null || value.equals("NULL")) continue;
            try {
                sum += (type == DataType.INTEGER) ? Long.parseLong(value) : Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new FileTypeException("Invalid numeric value in column '" + targetColumn + "': " + value);
            }
        }
        return sum;
    }

    public double avg(String tableName, String columnName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found: " + tableName);

        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        String targetColumn = (columnName == null || columnName.equals("*")) ? tableMeta.getColumnNames().get(0) : columnName;

        ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, targetColumn);
        DataType type = colMeta.getType();
        if (type != DataType.INTEGER && type != DataType.REAL)
            throw new FileTypeException("Cannot AVG non-numeric column: " + targetColumn);

        Column column = loadColumn(tableName, targetColumn);
        List<String> data = column.getData();
        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
        Set<Integer> matchingSet = (matchingRows == null) ? null : new HashSet<>(matchingRows);

        double sum = 0; int count = 0;
        for (int i = 0; i < data.size(); i++) {
            if (matchingSet != null && !matchingSet.contains(i)) continue;
            String value = data.get(i);
            if (value == null || value.equals("NULL")) continue;
            try {
                sum += (type == DataType.INTEGER) ? Long.parseLong(value) : Double.parseDouble(value);
                count++;
            } catch (NumberFormatException e) {
                throw new FileTypeException("Invalid numeric value in column '" + targetColumn + "': " + value);
            }
        }
        return (count == 0) ? 0 : sum / count;
    }

    public String min(String tableName, String columnName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found: " + tableName);
        ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, columnName);
        DataType type = colMeta.getType();
        Column column = loadColumn(tableName, columnName);
        List<String> data = column.getData();
        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
        Set<Integer> matchingSet = (matchingRows == null) ? null : new HashSet<>(matchingRows);
        String minValue = null;
        for (int i = 0; i < data.size(); i++) {
            if (matchingSet != null && !matchingSet.contains(i)) continue;
            String value = data.get(i);
            if (value == null || value.equals("NULL")) continue;
            if (minValue == null) { minValue = value; continue; }
            if (type == DataType.INTEGER && Long.parseLong(value) < Long.parseLong(minValue)) minValue = value;
            else if (type == DataType.REAL && Double.parseDouble(value) < Double.parseDouble(minValue)) minValue = value;
            else if (type != DataType.INTEGER && type != DataType.REAL && value.compareTo(minValue) < 0) minValue = value;
        }
        return minValue;
    }

    public String max(String tableName, String columnName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (!fileManager.tableExists(tableName)) throw new NoTableException("Table not found: " + tableName);
        ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, columnName);
        DataType type = colMeta.getType();
        Column column = loadColumn(tableName, columnName);
        List<String> data = column.getData();
        List<Integer> matchingRows = findMatchingRows(tableName, whereCol, whereOp, whereVal);
        Set<Integer> matchingSet = (matchingRows == null) ? null : new HashSet<>(matchingRows);
        String maxValue = null;
        for (int i = 0; i < data.size(); i++) {
            if (matchingSet != null && !matchingSet.contains(i)) continue;
            String value = data.get(i);
            if (value == null || value.equals("NULL")) continue;
            if (maxValue == null) { maxValue = value; continue; }
            if (type == DataType.INTEGER && Long.parseLong(value) > Long.parseLong(maxValue)) maxValue = value;
            else if (type == DataType.REAL && Double.parseDouble(value) > Double.parseDouble(maxValue)) maxValue = value;
            else if (type != DataType.INTEGER && type != DataType.REAL && value.compareTo(maxValue) > 0) maxValue = value;
        }
        return maxValue;
    }

    private List<Integer> findMatchingRows(String tableName, String whereCol, String whereOp, String whereVal) throws FileStorageException {
        if (whereCol == null) return null;

        String normalizedVal = normalizeValue(whereVal);
        String op = (whereOp != null) ? whereOp : "=";

        if ("=".equals(op) && !isTransaction && fileManager.indexExists(tableName, whereCol)) {
            ColumnIndex idx = fileManager.loadIndex(tableName, whereCol);
            return new ArrayList<>(idx.lookup(normalizedVal));
        }

        Column column = loadColumn(tableName, whereCol);
        List<String> data = column.getData();
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (matchesCondition(data.get(i), op, normalizedVal)) {
                result.add(i);
            }
        }
        return result;
    }

    private boolean matchesCondition(String dataValue, String op, String filterValue) {
        if (dataValue == null || dataValue.equals("NULL")) return false;
        if (filterValue == null) return false;

        boolean numeric = isNumeric(dataValue) && isNumeric(filterValue);
        int cmp = numeric
                ? Double.compare(Double.parseDouble(dataValue), Double.parseDouble(filterValue))
                : dataValue.compareTo(filterValue);

        return switch (op) {
            case "="        -> cmp == 0;
            case "!=", "<>" -> cmp != 0;
            case ">"        -> cmp > 0;
            case "<"        -> cmp < 0;
            case ">="       -> cmp >= 0;
            case "<="       -> cmp <= 0;
            default         -> false;
        };
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

    private Column createNewColumnWithNulls(String tableName) throws FileStorageException {
        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);
        int rowCount = 0;
        if (!tableMeta.getColumnNames().isEmpty()) {
            rowCount = loadColumn(tableName, tableMeta.getColumnNames().get(0)).getData().size();
        }
        Column newColumn = new Column();
        for (int i = 0; i < rowCount; i++) newColumn.addData(i, "NULL");
        return newColumn;
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
                case REAL    -> Double.parseDouble(value);
                case TEXT, BLOB, NULL -> { }
            }
        } catch (NumberFormatException e) {
            throw new Exception("Type mismatch: expected " + type + ", got '" + value + "'");
        }
    }

    private boolean evaluateSimpleCheck(String expression, String columnName, String value) {
        String normalized = expression.replaceAll("\\s+", "");
        String[] operators = {">=", "<=", "!=", "<>", "=", ">", "<"};
        for (String operator : operators) {
            int idx = normalized.indexOf(operator);
            if (idx <= 0) continue;
            String left  = normalized.substring(0, idx);
            String right = normalized.substring(idx + operator.length());
            if (!left.equals(columnName)) continue;
            return compareCheckValues(value, right, operator);
        }
        return true;
    }

    private boolean compareCheckValues(String value, String rightOperand, String operator) {
        boolean numericCompare = isNumeric(value) && isNumeric(rightOperand);
        int cmp = numericCompare
                ? Double.compare(Double.parseDouble(value), Double.parseDouble(rightOperand))
                : value.compareTo(rightOperand);
        return switch (operator) {
            case "="        -> cmp == 0;
            case "!=", "<>" -> cmp != 0;
            case ">"        -> cmp > 0;
            case "<"        -> cmp < 0;
            case ">="       -> cmp >= 0;
            case "<="       -> cmp <= 0;
            default         -> true;
        };
    }

    private boolean isNumeric(String value) {
        try { Double.parseDouble(value); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private void createEmptyIndex(String tableName, String columnName) throws FileStorageException {
        ColumnIndex index = new ColumnIndex();
        index.setColumnName(columnName);
        index.setIndex(new HashMap<>());
        fileManager.saveIndex(tableName, columnName, index);
    }

    private void rebuildIndexForColumn(String tableName, String columnName) throws FileStorageException {
        Column column = fileManager.loadColumnData(tableName, columnName);
        ColumnIndex index = new ColumnIndex();
        index.setColumnName(columnName);
        index.setIndex(new HashMap<>());

        List<String> data = column.getData();
        for (int i = 0; i < data.size(); i++) {
            String value = data.get(i);
            if (value != null && !value.equals("NULL")) {
                index.addEntry(value, i);
            }
        }

        fileManager.saveIndex(tableName, columnName, index);
    }

    private void rebuildIndexesForTable(String tableName) throws FileStorageException {
        TableMetadata tableMeta = fileManager.loadTableMetadata(tableName);

        for (String colName : tableMeta.getColumnNames()) {
            ColumnMetadata colMeta = fileManager.loadColumnMetadata(tableName, colName);

            boolean indexed =
                    colMeta.getConstraints().contains(Constraints.PRIMARY_KEY)
                            || colMeta.getConstraints().contains(Constraints.UNIQUE);

            if (indexed) {
                rebuildIndexForColumn(tableName, colName);
            } else if (fileManager.indexExists(tableName, colName)) {
                fileManager.deleteIndex(tableName, colName);
            }
        }
    }

    private void updateIndexesAfterInsert(String tableName,
                                          Map<String, String> insertedValues,
                                          int rowIndex) throws FileStorageException {
        for (Map.Entry<String, String> entry : insertedValues.entrySet()) {
            String colName = entry.getKey();
            String value = entry.getValue();

            if (!fileManager.indexExists(tableName, colName)) {
                continue;
            }

            ColumnIndex index = fileManager.loadIndex(tableName, colName);
            index.addEntry(value, rowIndex);
            fileManager.saveIndex(tableName, colName, index);
        }
    }
}
