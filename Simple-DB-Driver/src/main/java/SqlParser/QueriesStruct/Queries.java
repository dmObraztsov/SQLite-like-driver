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
            return fileManager.setNameDB(databaseName);
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
            for(Column curr : tableColumns)
            {
                flag = fileManager.saveColumn(tableName, curr);
                if(!flag)
                {
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
}
