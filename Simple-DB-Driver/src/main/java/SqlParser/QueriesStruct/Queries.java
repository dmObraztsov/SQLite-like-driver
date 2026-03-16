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

    public record SelectDataQuery(List<String> selectCols, boolean isStar, String tableName, String whereName, String whereValue) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> results = engine.select(tableName, selectCols, isStar, whereName, whereValue);
            return new ExecutionResult(true, "Select completed.", results);
        }
    }

    public record JoinTableQuery(String table1Name, List<String> columns1, String table2Name, List<String> columns2, String leftJoinCol, String rightJoinCol) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> result = engine.join(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol);
            return new ExecutionResult(true, "Join completed", result);
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

}