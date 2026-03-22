package SqlParser.QueriesStruct;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;

import java.util.*;

public class Queries {

    public record CreateDataBaseQuery(String databaseName) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.createDatabase(databaseName);
            return new ExecutionResult(true, "Database '" + databaseName + "' created.");
        }
    }

    public record DropDataBaseQuery(String databaseName) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.dropDatabase(databaseName);
            return new ExecutionResult(true, "Database '" + databaseName + "' dropped.");
        }
    }

    public record UseDataBaseQuery(String databaseName) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws FileStorageException {
            engine.setCurrentDatabase(databaseName);
            return new ExecutionResult(true, "Switched to database '" + databaseName + "'.");
        }
    }

    public record CreateTableQuery(String tableName, List<ColumnMetadata> columns) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.createTable(tableName, columns);
            return new ExecutionResult(true, "Table '" + tableName + "' created successfully.");
        }
    }

    public record DropTableQuery(String tableName) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.dropTable(tableName);
            return new ExecutionResult(true, "Table '" + tableName + "' dropped successfully.");
        }
    }

    public record InsertTableQuery(String tableName, List<String> columnNames, List<String> values) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.insert(tableName, columnNames, values);
            return new ExecutionResult(true, "Row inserted into '" + tableName + "'.");
        }
    }

    public record SelectDataQuery(List<String> selectCols, boolean isStar, String tableName, String whereName, String whereValue, boolean isDistinct) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> results = engine.select(tableName, selectCols, isStar, whereName, whereValue, isDistinct);
            return new ExecutionResult(true, "Select completed.", results);
        }
    }

    public record JoinTableQuery(String table1Name, List<String> columns1, String table2Name, List<String> columns2, String leftJoinCol, String rightJoinCol, boolean isDistinct) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> rawJoinedRows = engine.join(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol);

            List<Row> projectedRows = new ArrayList<>();

            for (Row row : rawJoinedRows) {
                if (columns1 == null && columns2 == null) {
                    projectedRows.add(row);
                    continue;
                }

                Map<String, String> filteredValues = new HashMap<>();

                if (columns1 != null) {
                    for (String col : columns1) {
                        String fullKey = table1Name + "." + col;
                        String value = row.get(fullKey);
                        if (value != null) {
                            filteredValues.put(fullKey, value);
                            if (!col.equalsIgnoreCase("id")) {
                                filteredValues.putIfAbsent(col, value);
                            }
                        }
                    }
                }

                if (columns2 != null) {
                    for (String col : columns2) {
                        String fullKey = table2Name + "." + col;
                        String value = row.get(fullKey);
                        if (value != null) {
                            filteredValues.put(fullKey, value);
                            if (!col.equalsIgnoreCase("id")) {
                                filteredValues.putIfAbsent(col, value);
                            }
                        }
                    }
                }

                projectedRows.add(new Row(filteredValues));
            }

            List<Row> finalResult = projectedRows;
            if (this.isDistinct) {
                finalResult = new ArrayList<>(new LinkedHashSet<>(projectedRows));
            }

            return new ExecutionResult(true, "Join completed", finalResult);
        }
    }

    public record BeginTransactionQuery() implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) {
            engine.beginTransaction();
            return new ExecutionResult(true, "Transaction started.");
        }
    }

    public record CommitQuery() implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.commit();
            return new ExecutionResult(true, "Transaction committed.");
        }
    }

    public record RollbackQuery() implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) {
            engine.rollback();
            return new ExecutionResult(true, "Transaction rolled back.");
        }
    }

    public record AlterTableAddColumnQuery(String tableName, ColumnMetadata column) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableAddColumn(tableName, column);
            return new ExecutionResult(true, "Column '" + column.getName() + "' added to table '" + tableName + "'.");
        }
    }

    public record AlterTableDropColumnQuery(String tableName, String columnName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableDropColumn(tableName, columnName);
            return new ExecutionResult(true, "Column '" + columnName + "' dropped from table '" + tableName + "'.");
        }
    }

    public record AlterTableRenameColumnQuery(String tableName, String columnName, String newName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableRenameColumn(tableName, columnName, newName);
            return new ExecutionResult(true, "Column '" + columnName + "' renamed to '" + newName + "'.");
        }
    }

    public record AlterTableRenameTableQuery(String tableName, String newName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableRenameTable(tableName, newName);
            return new ExecutionResult(true, "Table '" + tableName + "' renamed to '" + newName + "'.");
        }
    }

    public record DeleteTableQuery(String tableName, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int deletedRows = engine.delete(tableName, whereCol, whereValue);
            String message = (deletedRows > 0) ? deletedRows + " row(s) deleted from '" + tableName + "'." : "No matching rows found in '" + tableName + "'.";
            return new ExecutionResult(true, message);
        }
    }

    public record UpdateTableQuery(String tableName, Map<String, String> setValues, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int updatedRows = engine.update(tableName, setValues, whereCol, whereValue);
            String message = (updatedRows > 0) ? updatedRows + " row(s) updated in '" + tableName + "'." : "No matching rows found in '" + tableName + "'.";
            return new ExecutionResult(true, message);
        }
    }

    public record CountQuery(String tableName, String columnName, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int count = engine.count(tableName, columnName, whereCol, whereValue);
            return new ExecutionResult(true, "Count result: " + count, List.of(new Row(Map.of("COUNT", String.valueOf(count)))));
        }
    }

    public record SumQuery(String tableName, String columnName, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            double sum = engine.sum(tableName, columnName, whereCol, whereValue);
            return new ExecutionResult(true, "Sum result: " + sum, List.of(new Row(Map.of("SUM", String.valueOf(sum)))));
        }
    }

    public record AvgQuery(String tableName, String columnName, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            double avg = engine.avg(tableName, columnName, whereCol, whereValue);
            return new ExecutionResult(true, "Avg result: " + avg, List.of(new Row(Map.of("AVG", String.valueOf(avg)))));
        }
    }

    public record MinQuery(String tableName, String columnName, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            String min = engine.min(tableName, columnName, whereCol, whereValue);
            return new ExecutionResult(true, "Min result: " + min, List.of(new Row(Map.of("MIN", min))));
        }
    }

    public record MaxQuery(String tableName, String columnName, String whereCol, String whereValue) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            String max = engine.max(tableName, columnName, whereCol, whereValue);
            return new ExecutionResult(true, "Max result: " + max, List.of(new Row(Map.of("MAX", max))));
        }
    }

}