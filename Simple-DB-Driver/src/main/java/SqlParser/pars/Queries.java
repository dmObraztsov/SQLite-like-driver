package SqlParser.pars;

public class Queries {

    public static class CreateDataBaseQuery implements QueryInterface {
        private final String tableName;

        CreateDataBaseQuery(String tableName)
        {
            this.tableName = tableName;
        }

        @Override
        public boolean execute() {
            return true;
        }

        @Override
        public String getStringVision() {
            return "Creating database with mame " + "\"" + tableName + "\"";
        }
    }

    public static class DropDataBaseQuery implements QueryInterface
    {

        @Override
        public boolean execute() {
            return false;
        }

        @Override
        public String getStringVision() {
            return "";
        }
    }

    public static class UseDataBaseQuery implements QueryInterface
    {

        @Override
        public boolean execute() {
            return false;
        }

        @Override
        public String getStringVision() {
            return "";
        }
    }

    public static class CreateTableQuery implements QueryInterface
    {

        @Override
        public boolean execute() {
            return false;
        }

        @Override
        public String getStringVision() {
            return "";
        }
    }

    public static class DropTableQuery implements QueryInterface
    {

        @Override
        public boolean execute() {
            return false;
        }

        @Override
        public String getStringVision() {
            return "";
        }
    }

    public static class AlterTableQuery implements QueryInterface
    {

        @Override
        public boolean execute() {
            return false;
        }

        @Override
        public String getStringVision() {
            return "";
        }
    }

    public static class InsertRowQuery implements QueryInterface
    {

        @Override
        public boolean execute() {
            return false;
        }

        @Override
        public String getStringVision() {
            return "";
        }
    }


    /**
     * Создаем под каждый запрос отдельный класс, будет очень удобно потом работать
     * Позже будем передавать в DatabaseManager объект запроса который внутри себя будет работать
     * Хороший уровень абстракции и отлаживать будет удобно
     */
}
