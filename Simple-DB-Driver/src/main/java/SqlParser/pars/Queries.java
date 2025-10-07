package SqlParser.pars;

public class Queries {
    public interface Query {}

    public class CreateTableQuery implements Query {
        private String tableName;
        private List<ColumnDefinition> columns;
    }

    /**
     * Создаем под каждый запрос отдельный класс, будет очень удобно потом работать
     * Позже будем передавать в DatabaseManager объект запроса который внутри себя будет работать
     * Хороший уровень абстракции и отлаживать будет удобно
     */
}
