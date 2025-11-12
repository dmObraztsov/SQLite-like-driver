package SqlParser.QueriesStruct;

import FileWork.FileManager;
import Yadro.JournalManager;

public class Queries {

    public static class CreateDataBaseQuery implements QueryInterface {
        private final String databaseName;

        public CreateDataBaseQuery(String databaseName) {
            this.databaseName = databaseName;
        }

        @Override
        public void execute(FileManager fileManager) {
            fileManager.createDB(databaseName);
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
        public void execute(FileManager fileManager) {
            fileManager.dropDB(databaseName);
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
        public void execute(FileManager fileManager) {
            fileManager.setNameDB(databaseName);
        }

        @Override
        public String getStringVision() {
            return "Use database with mame " + "\"" + databaseName + "\"";
        }
    }

    public static class CreateTableQuery implements QueryInterface
    {
        private final String tableName;

        public CreateTableQuery(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public void execute(FileManager fileManager) {
            fileManager.createTable(tableName);
        }

        @Override
        public String getStringVision() {
            return "Creating table with mame " + "\"" + tableName + "\"";
        }
    }
//
//    public static class DropTableQuery implements QueryInterface
//    {
//
//        @Override
//        public boolean execute() {
//            return false;
//        }
//
//        @Override
//        public String getStringVision() {
//            return "";
//        }
//    }
//
//    public static class AlterTableQuery implements QueryInterface
//    {
//
//        @Override
//        public boolean execute() {
//            return false;
//        }
//
//        @Override
//        public String getStringVision() {
//            return "";
//        }
//    }
//
//    public static class InsertRowQuery implements QueryInterface
//    {
//
//        @Override
//        public boolean execute() {
//            return false;
//        }
//
//        @Override
//        public String getStringVision() {
//            return "";
//        }
//    }
}
