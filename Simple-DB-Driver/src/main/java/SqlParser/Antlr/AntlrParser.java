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
        ArrayList<Column> columns = new ArrayList<>();
        for(SQLParser.ColumnContext curr : ctx.column())
        {
            DataType dataType;
            ArrayList<Constraints> constraints = new ArrayList<>();
            Collate collate = null; //TODO

            dataType = switch (curr.TYPE().getText()) {
                case "INTEGER" -> DataType.INTEGER;
                case "REAL" -> DataType.REAL;
                case "TEXT" -> DataType.TEXT;
                case "BLOB" -> DataType.BLOB;
                default -> null;
            };

            for(SQLParser.ConstraintContext currConstraint : curr.constraint())
            {
                Constraints constraint = getConstraints(currConstraint);

                if(constraint != null)
                {
                    constraints.add(constraint);
                }
            }

            columns.add(new Column(dataType, curr.name().getText(), constraints, collate));
        }

        return new Queries.CreateTableQuery(tableName, columns);
    }

    @Override
    public QueryInterface visitDropTableStatement(SQLParser.DropTableStatementContext ctx) {
        return new Queries.DropTableQuery(ctx.name().getText());
    }

    private static Constraints getConstraints(SQLParser.ConstraintContext currConstraint) {
        Constraints constraint;
        String text = currConstraint.getText();
        constraint = switch (text) {
        case "NOTNULL" -> Constraints.NOT_NULL;
        case "PRIMARYKEY" -> Constraints.PRIMARY_KEY;
        case "AUTOINCREMENT" -> Constraints.AUTOINCREMENT;
        case "UNIQUE" -> Constraints.UNIQUE;
        case "NULL" -> Constraints.NULL;
        case "CHECK" -> Constraints.CHECK;
        case  "DEFAULT" -> Constraints.DEFAULT;
        default ->  null;
        };
        return constraint;
    }
}
