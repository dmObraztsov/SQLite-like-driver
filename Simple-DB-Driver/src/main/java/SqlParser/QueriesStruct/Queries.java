package SqlParser.QueriesStruct;

import FileWork.FileManager;
import Yadro.DataStruct.Column;
import Yadro.JournalManager;

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
        private final ArrayList<Column> tableColumns = new ArrayList<>();

        public CreateTableQuery(String tableName, ArrayList<Column> tableColumns) {
            this.tableName = tableName;
            this.tableColumns.addAll(tableColumns);
        }

        @Override
        public boolean execute(FileManager fileManager) {
            boolean flag = fileManager.createTable(tableName);
            if(!flag)
            {
                fileManager.dropTable(tableName);
                return flag;
            }

            for(Column curr : tableColumns)
            {
                flag = fileManager.saveColumn(tableName, curr);
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
            private final Column column;

            public AlterAddColumnQuery(String tableName, Column column) {
                super(tableName);
                this.column = column;
            }

            @Override
            public boolean execute(FileManager fileManager) {
                return fileManager.saveColumn(super.tableName, this.column);
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
}
