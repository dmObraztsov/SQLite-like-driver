package SqlParser.pars;

public class AntlrParser extends SqlParser.pars.SQLBaseVisitor<QueryInterface> {
    @Override
    public QueryInterface visitQuery(SQLParser.QueryContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public QueryInterface visitCreateDBStatement(SQLParser.CreateDBStatementContext ctx) {
        return new Queries.CreateDataBaseQuery(ctx.tableName().getText());
    }

    @Override
    public QueryInterface visitDropDBStatement(SQLParser.DropDBStatementContext ctx)
    {
        return new Queries.DropDataBaseQuery(ctx.tableName().getText());
    }
}
