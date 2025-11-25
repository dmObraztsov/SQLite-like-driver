package SqlParser.QueriesStruct;

import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.DataType;

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
            boolean flag = fileManager.createTable(tableName);
            if(!flag)
            {
                return flag;
            }

            for(ColumnMetadata curr : tableColumns)
            {
                flag = fileManager.createColumn(tableName, curr);
                if(!flag)
                {
                    fileManager.dropTable(tableName);
                    return flag;
                }
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
            boolean flag = true;

            if(columns.isEmpty())
            {
                TableMetadata tableMetadata = fileManager.getTableMetadata(tableName);
                columns.addAll(tableMetadata.getColumnNames());
                if(columns.size() != values.size()) return false;
            }

            if(columns.size() != values.size())
            {
                //Use autoincrement to missing columns if they exist
            }

            else
            {
                for(int i = 0; i < columns.size(); i++)
                {
                    Column column = fileManager.loadColumn(tableName, columns.get(i));
                    ColumnMetadata columnMetadata = fileManager.loadColumnMetadata(tableName, columns.get(i));
                    DataType dataType = providedType(values.get(i));
                    if(dataType == null ||
                            !dataType.equals(columnMetadata.getType()) ||
                            !satisfiesConstraints(columnMetadata, values.get(i))) {
                        return false;
                    }

                    column.addData(values.get(i));
                    columnMetadata.setSize(columnMetadata.getSize() + 1);

                    flag = fileManager.saveColumn(tableName, columns.get(i), column) &
                            fileManager.saveColumnMetadata(tableName, columns.get(i), columnMetadata);
                }
            }

            return flag;
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

        private boolean satisfiesConstraints(ColumnMetadata columnMetadata, String content) {
            return true;
        }

        private boolean isDecimal(String str) {
            return str != null && str.matches("-?\\d+(\\.\\d+)?");
        }
    }
}
