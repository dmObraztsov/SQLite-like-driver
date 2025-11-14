package SqlParser.Antlr;

import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

import java.util.ArrayList;

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
    public QueryInterface visitDropDBStatement(SQLParser.DropDBStatementContext ctx) {
        return new Queries.DropDataBaseQuery(ctx.name().getText());
    }

    @Override
    public QueryInterface visitUseDBStatement(SQLParser.UseDBStatementContext ctx) {
        return new Queries.UseDataBaseQuery(ctx.name().getText());
    }

    @Override
    public QueryInterface visitCreateTableStatement(SQLParser.CreateTableStatementContext ctx) {
        String tableName = ctx.name().getText();
        ArrayList<Column> columns  = new ArrayList<>();
        for(SQLParser.ColumnContext curr : ctx.column())
        {
            DataType dataType;
            Constraints constraints; //TODO
            Collate collate; //TODO

            dataType = switch (curr.TYPE().getText()) {
                case "INTEGER" -> DataType.INTEGER;
                case "REAL" -> DataType.REAL;
                case "TEXT" -> DataType.TEXT;
                case "BLOB" -> DataType.BLOB;
                default -> DataType.NULL;
            };

            Column column = new Column(dataType, curr.name().getText());
            columns.add(column);
        }

        return new Queries.CreateTableQuery(tableName, columns);
    }
}
