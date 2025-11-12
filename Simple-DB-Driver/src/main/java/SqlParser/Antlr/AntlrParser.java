package SqlParser.Antlr;

import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;

/**
 * Про то как тут все работает, лучше мне лично объяснить, но вкратце - библиотека генерит свои классы, а нам в них
 * просто нужно добавить реализацию прохода по дереву токенов
 */

public class AntlrParser extends SQLBaseVisitor<QueryInterface> {
    @Override
    public QueryInterface visitQuery(SQLParser.QueryContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public QueryInterface visitCreateDBStatement(SQLParser.CreateDBStatementContext ctx) {
        return new Queries.CreateDataBaseQuery(ctx.name().getText());
    }

    @Override
    public QueryInterface visitCreateTableStatement(SQLParser.CreateTableStatementContext ctx) {
        return new Queries.CreateTableQuery(ctx.name().getText());
    }

    @Override
    public QueryInterface visitDropDBStatement(SQLParser.DropDBStatementContext ctx) {
        return new Queries.DropDataBaseQuery(ctx.name().getText());
    }

    @Override
    public QueryInterface visitUseDBStatement(SQLParser.UseDBStatementContext ctx) {
        return new Queries.UseDataBaseQuery(ctx.name().getText());
    }
}
