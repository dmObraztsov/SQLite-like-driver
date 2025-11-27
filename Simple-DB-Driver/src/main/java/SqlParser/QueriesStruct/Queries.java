package SqlParser.QueriesStruct;

import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.*;

import java.util.ArrayList;
import java.util.Objects;

public class Queries {

    public static class CreateDataBaseQuery implements QueryInterface
    {
        private final String databaseName;

        public CreateDataBaseQuery(String databaseName) {
            this.databaseName = databaseName;
        }

        @Override
        public boolean execute(FileManager fileManager) {
            return fileManager.createDB(databaseName);
        }

        @Override
        public String getStringVision() {
            return "Creating database with mame " + "\"" + databaseName + "\"";
        }
    }

    public static class DropDataBaseQuery implements QueryInterface
    {
        private final String databaseName;

        public DropDataBaseQuery(String databaseName)
        {
            this.databaseName = databaseName;
        }

        @Override
        public boolean execute(FileManager fileManager) {
            return fileManager.dropDB(databaseName);
        }

        @Override
        public String getStringVision() {
            return "Drop database with mame " + "\"" + databaseName + "\"";
        }
    }

    public static class UseDataBaseQuery implements QueryInterface
    {
        private final String databaseName;

        public UseDataBaseQuery(String databaseName)
        {
            this.databaseName = databaseName;
        }

        @Override
        public boolean execute(FileManager fileManager) {
            return fileManager.useDB(databaseName);
        }

        @Override
        public String getStringVision() {
            return "Use database with mame " + "\"" + databaseName + "\"";
        }
    }

    public static class CreateTableQuery implements QueryInterface
    {
        private final String tableName;
        private final ArrayList<ColumnMetadata> tableColumns = new ArrayList<>();

        public CreateTableQuery(String tableName, ArrayList<ColumnMetadata> tableColumns) {
            this.tableName = tableName;
            this.tableColumns.addAll(tableColumns);
        }

        @Override
        public boolean execute(FileManager fileManager) {
            boolean flag;

            flag = fileManager.createTable(tableName);
            if(!flag) return flag;

            flag = fileManager.createPrimaryKeyMap(tableName);
            if(!flag)
            {
                return flag;
            }

            boolean contPrimaryKey = false;
            for(ColumnMetadata curr : tableColumns)
            {
                flag = fileManager.createColumn(tableName, curr);
                if(!flag)
                {
                    fileManager.dropTable(tableName);
                    return flag;
                }

                if(curr.getConstraints().contains(Constraints.PRIMARY_KEY)) contPrimaryKey = true;
            }

            if(!contPrimaryKey) {
                ArrayList<Constraints> constraints = new ArrayList<>();
                constraints.add(Constraints.UNIQUE);
                constraints.add(Constraints.NOT_NULL);
                constraints.add(Constraints.AUTOINCREMENT);
                constraints.add(Constraints.PRIMARY_KEY);

                ColumnMetadata primaryKey = new ColumnMetadata("_id", DataType.INTEGER, 0, constraints, null);
                fileManager.createColumn(tableName, primaryKey);
            }

            return flag;
        }

        @Override
        public String getStringVision() {
            return "Creating table with mame " + "\"" + tableName + "\"";
        }
    }

    public static class DropTableQuery implements QueryInterface
    {
        private final String tableName;

        public DropTableQuery(String tableName)
        {
            this.tableName = tableName;
        }

        @Override
        public boolean execute(FileManager fileManager) {
            return fileManager.dropTable(tableName);
        }

        @Override
        public String getStringVision() {
            return "Drop table with mame " + "\"" + tableName + "\"";
        }
    }

    public static class AlterTableQuery implements QueryInterface
    {
        protected final String tableName;

        public AlterTableQuery(String tableName)
        {
            this.tableName = tableName;
        }

        @Override
        public boolean execute(FileManager fileManager) {
            return false;
        }

        @Override
        public String getStringVision() {
            return "Alter table with mame " + "\"" + tableName + "\"";
        }

        public static class AlterRenameTableQuery extends AlterTableQuery
        {
            private final String changeTableName;

            public AlterRenameTableQuery(String tableName, String changeTableName) {
                super(tableName);
                this.changeTableName = changeTableName;
            }

            @Override
            public boolean execute(FileManager fileManager) {
                return fileManager.renameTable(super.tableName, this.changeTableName);
            }

            @Override
            public String getStringVision() {
                return "Rename table with mame " + "\"" + super.tableName + "to " + this.changeTableName + "\"";
            }
        }

        public static class AlterAddColumnQuery extends AlterTableQuery
        {
            private final ColumnMetadata column;

            public AlterAddColumnQuery(String tableName, ColumnMetadata column) {
                super(tableName);
                this.column = column;
            }

            @Override
            public boolean execute(FileManager fileManager) {
                return fileManager.createColumn(super.tableName, this.column);
            }

            @Override
            public String getStringVision() {
                return "Add column to table with mame " + "\"" + super.tableName + "\"";
            }
        }

        public static class AlterDropColumnQuery extends AlterTableQuery
        {
            private final String dropColumnName;

            public AlterDropColumnQuery(String tableName, String dropColumnName) {
                super(tableName);
                this.dropColumnName = dropColumnName;
            }

            @Override
            public boolean execute(FileManager fileManager) {
                return fileManager.deleteColumn(super.tableName, this.dropColumnName);
            }

            @Override
            public String getStringVision() {
                return "Add column to table with mame " + "\"" + super.tableName + "\"";
            }
        }

        public static class AlterRenameColumnQuery extends AlterTableQuery
        {
            private final String renameColumnName;
            private final String columnName;

            public AlterRenameColumnQuery(String tableName, String columnName, String renameColumnName) {
                super(tableName);
                this.renameColumnName = renameColumnName;
                this.columnName = columnName;
            }

            @Override
            public boolean execute(FileManager fileManager) {
                return fileManager.renameColumn(super.tableName, columnName, renameColumnName);
            }

            @Override
            public String getStringVision() {
                return "Add column to table with mame " + "\"" + super.tableName + "\"";
            }
        }
    }

    public static class InsertTableQuery implements QueryInterface
    {
        private final String tableName;
        private final ArrayList<String> columns;
        private final ArrayList<String> values;

        public InsertTableQuery(String tableName, ArrayList<String> columns, ArrayList<String> values)
        {
            this.tableName = tableName;
            this.columns = new ArrayList<>(columns);
            this.values = new ArrayList<>(values);
        }

        @Override
        public boolean execute(FileManager fileManager) {
            initializeColumnsIfEmpty(fileManager);

            ArrayList<Object> primaryKeys = processColumnsAndCollectPrimaryKeys(fileManager);
            workPrimaryKey(primaryKeys, fileManager);

            return true;
        }

        @Override
        public String getStringVision() {
            return "";
        }

        private DataType providedType(String content) {
            if(content.equals("NULL")) return DataType.NULL;
            if(content.matches("-?\\d+")) return DataType.INTEGER;
            if(content.matches("-?\\d+(\\.\\d+)?")) return DataType.REAL;
            if(content.startsWith("\"") && content.endsWith("\"") && content.length() >= 2) return DataType.TEXT;
            else return null;
        }

        private boolean checkConstraints(Column column, ColumnMetadata columnMetadata, String content) {
            for(Constraints curr : columnMetadata.getConstraints()) {
                switch (curr) {
                    case UNIQUE:
                        if(column.getData().contains(content)) return false;
                        break;
                    case NOT_NULL:
                        if(content == null) return false;
                        break;
                    case CHECK:                        //TODO
                        break;
                    case DEFAULT:                        //TODO
                        break;
                }
            }

            return true;
        }

        private void appendDataToColumn(Column column, ColumnMetadata columnMetadata, String data, FileManager fileManager) {
            if(columnMetadata.getType() == providedType(data) && checkConstraints(column, columnMetadata, data)) {
                column.addData(data);
                columnMetadata.incrementSize();

                fileManager.saveColumn(tableName, columnMetadata.getName(), column);
                fileManager.saveColumnMetadata(tableName, columnMetadata.getName(), columnMetadata);
            }
        }

        private void workPrimaryKey(ArrayList<Object> primaryKeys, FileManager fileManager) {
            PrimaryKeyMap primaryKeyMap = fileManager.loadPrimaryKeyMap(tableName);
            primaryKeyMap.addLink(primaryKeys, (Integer) (primaryKeys.getFirst()));


            fileManager.savePrimaryKeyMap(tableName, primaryKeyMap);
        }

        private void initializeColumnsIfEmpty(FileManager fileManager) {
            if (columns.isEmpty()) {
                TableMetadata tableMetadata = fileManager.loadTableMetadata(tableName);
                columns.addAll(tableMetadata.getColumnNames());
            }
        }

        private ArrayList<Object> processColumnsAndCollectPrimaryKeys(FileManager fileManager) {
            ArrayList<Object> primaryKeys = new ArrayList<>();
            int valueIndex = 0;

            for (String columnName : columns) {
                Column column = fileManager.loadColumn(tableName, columnName);
                ColumnMetadata metadata = fileManager.loadColumnMetadata(tableName, columnName);

                if (isAutoIncrementPrimaryKey(metadata)) {
                    Object primaryKeyValue = processAutoIncrementColumn(column, metadata, fileManager);
                    primaryKeys.add(primaryKeyValue);
                } else {
                    processRegularColumn(column, metadata, values.get(valueIndex++), fileManager);
                }
            }

            return primaryKeys;
        }

        private boolean isAutoIncrementPrimaryKey(ColumnMetadata metadata) {
            return metadata.getConstraints().contains(Constraints.PRIMARY_KEY) &&
                    metadata.getConstraints().contains(Constraints.AUTOINCREMENT) &&
                    metadata.getType() == DataType.INTEGER;
        }

        private Object processAutoIncrementColumn(Column column, ColumnMetadata metadata,
                                                  FileManager fileManager) {
            int lastValue = getLastColumnValue(column);
            int newValue = lastValue + 1;

            appendDataToColumn(column, metadata, String.valueOf(newValue), fileManager);

            return newValue;
        }

        private void processRegularColumn(Column column, ColumnMetadata metadata,
                                          Object value, FileManager fileManager) {
            appendDataToColumn(column, metadata, String.valueOf(value), fileManager);
        }

        private int getLastColumnValue(Column column) {
            if (column.getData().isEmpty()) {
                return 0;
            }

            return Integer.parseInt(column.getData().getLast());
        }
    }
}
