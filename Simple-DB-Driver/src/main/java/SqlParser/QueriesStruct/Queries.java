package SqlParser.QueriesStruct;

import Exceptions.*;
import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.*;
import java.util.ArrayList;

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

            flag = fileManager.createTable(tableName) &
                    fileManager.createPrimaryKeyMap(tableName);

            if (!flag) return false;

            for (ColumnMetadata curr : tableColumns) {
                try {
                    if (!fileManager.createColumn(tableName, curr)) {
                        fileManager.dropTable(tableName);
                        return false;
                    }
                } catch (FileStorageException e) {
                    fileManager.dropTable(tableName);
                    if (e instanceof NoFileException) {
                        System.err.println("File not found: " + e.getMessage());
                    } else if (e instanceof EmptyFileException) {
                        System.err.println("Empty file: " + e.getMessage());
                    } else if (e instanceof PermissionDeniedException) {
                        System.err.println("Permission denied: " + e.getMessage());
                    } else if (e instanceof SerializationStorageException) {
                        System.err.println("Deserialization error: " + e.getMessage());
                    }
                    return false;
                }
            }

            try {
                fileManager.createId(tableName);
            } catch (FileStorageException e) {
                fileManager.dropTable(tableName);
                return false;
            }

            return true;
        }


        /*
        public boolean execute(FileManager fileManager) {
            boolean flag;
            flag = fileManager.createTable(tableName) & fileManager.createPrimaryKeyMap(tableName);
            if(!flag) return false;

            for(ColumnMetadata curr : tableColumns)
            {

                if(!fileManager.createColumn(tableName, curr))
                {
                    fileManager.dropTable(tableName);
                    return false;
                }
            }

            flag = fileManager.createId(tableName);

            return flag;
        }
        */


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
        private ArrayList<String> columns;
        private final ArrayList<String> values;

        public InsertTableQuery(String tableName, ArrayList<String> columns, ArrayList<String> values)
        {
            this.tableName = tableName;
            this.columns = new ArrayList<>(columns);
            this.values = new ArrayList<>(values);
        }

        public record ColumnValue(boolean isPrimaryKey, String value, String column) {
        }

        @Override
        public boolean execute(FileManager fileManager) {
            //TODO пока вставляю все колонки, но потом нужно использовать только те что из запроса и понимать
            // какие можно вставить NULL, а какие нельзя и бросить исключение если не сходится кол-во
            TableMetadata tableMetadata = fileManager.loadTableMetadata(tableName);
            columns = new ArrayList<>(tableMetadata.getColumnNames());

            if(values.size() + countAutoincrement(fileManager) + countDefault(fileManager) != columns.size()) return false;

            ArrayList<ColumnValue> checkedRowToAdd = new ArrayList<>();
            int j = 0;

            for(String curr : columns) {
                Column column = fileManager.loadColumn(tableName, curr);
                ColumnMetadata columnMetadata = fileManager.loadColumnMetadata(tableName, curr);

                boolean isPrimaryKey = false;
                String value;

                if(columnMetadata.getConstraints().contains(Constraints.PRIMARY_KEY)) {
                    isPrimaryKey = true;
                }

                if(columnMetadata.getConstraints().contains(Constraints.DEFAULT)) {
                    value = "SOME DEFAULT VALUE";
                    //TODO иногда нужно вставить значение из values но я хз как понять когда надо, а когда не надо
                }

                else if(columnMetadata.getConstraints().contains(Constraints.AUTOINCREMENT)) {
                    if(column.getData().isEmpty()) {
                        value = "1";
                    }

                    else {
                        value = String.valueOf(Integer.parseInt(column.getData().getLast()) + 1);
                    }

                }

                else {
                    value = values.get(j++);
                }

                if(fullCheck(column, columnMetadata, value)) {
                    checkedRowToAdd.add(new ColumnValue(isPrimaryKey, value, curr));

                }
            }

            return IndexWorker.AddToDataBase(checkedRowToAdd, fileManager, tableName);
        }

        @Override
        public String getStringVision() {
            return "";
        }

        private boolean fullCheck(Column column, ColumnMetadata columnMetadata, String content) {
            return (providedType(content) == columnMetadata.getType()) && checkConstraints(column, columnMetadata, content);
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
                }
            }

            return true;
        }

        private int countAutoincrement(FileManager fileManager) {
            return fileManager.loadTableMetadata(tableName).getCountAutoIncrements();
        }

        private int countDefault(FileManager fileManager) {
            return fileManager.loadTableMetadata(tableName).getCountDefaults();
        }
    }
}
