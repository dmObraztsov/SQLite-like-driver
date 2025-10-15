package SqlParser.pars;

/**
 * Про то как тут все работает, лучше мне лично объяснить, но вкратце - библиотека генерит свои классы, а нам в них
 * просто нужно добавить реализацию прохода по дереву токенов
 */

public class AntlrParser extends SqlParser.pars.SQLBaseVisitor<QueryInterface> {
    @Override
    public QueryInterface visitQuery(SQLParser.QueryContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public QueryInterface visitCreateDBStatement(SQLParser.CreateDBStatementContext ctx) {
        return new Queries.CreateDataBaseQuery(ctx.tableName().getText());
    }
}
