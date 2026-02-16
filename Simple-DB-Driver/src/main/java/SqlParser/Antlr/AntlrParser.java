package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import org.antlr.v4.runtime.RuleContext;

import java.util.ArrayList;
import java.util.List;

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
        ArrayList<ColumnMetadata> columns = new ArrayList<>();
        for(SQLParser.ColumnContext curr : ctx.column())
        {
            columns.add(parseColumn(curr));
        }

        return new Queries.CreateTableQuery(tableName, columns);
    }

    @Override
    public QueryInterface visitDropTableStatement(SQLParser.DropTableStatementContext ctx) {
        return new Queries.DropTableQuery(ctx.name().getText());
    }

    @Override
    public QueryInterface visitInsertTableStatement(SQLParser.InsertTableStatementContext ctx) {
        String tableName = ctx.tablename().getText();
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        for(int i = 0; i < ctx.name().size(); i++) {
            columns.add(ctx.name().get(i).getText());
        }

        for(int i = 0; i < ctx.data().size(); i++) {
            values.add(ctx.data().get(i).getText());
        }

        return new Queries.InsertTableQuery(tableName, columns, values);
    }

    @Override
    public QueryInterface visitSelectDataStatement(SQLParser.SelectDataStatementContext ctx) {
        boolean isStar = ctx.selectCols().STAR() != null;

        List<String> columns = null;
        SQLParser.WhereClauseContext whereClause = ctx.whereClause();
        String tableName = ctx.tablename().getText();

        if (!isStar) {
            columns = ctx.selectCols().name().stream().map(RuleContext::getText).toList();
        }

        if (whereClause != null) return new Queries.SelectDataQuery(columns, isStar, tableName, whereClause.name().getText(), whereClause.value().getText());
        else return new Queries.SelectDataQuery(columns, isStar, tableName, null, null);
    }

    @Override
    public QueryInterface visitJoinTableStatement(SQLParser.JoinTableStatementContext ctx) {
        String table1Name = ctx.tablename(0).getText();
        String table2Name = ctx.tablename(1).getText();

        List<String> columns1 = new ArrayList<>();
        List<String> columns2 = new ArrayList<>();

        columns1.add(ctx.joinCols().longName(0).NAME(1).getText());
        columns2.add(ctx.joinCols().longName(1).NAME(1).getText());

        String leftJoinCol = ctx.onClause().longName(0).NAME(1).getText();
        String rightJoinCol = ctx.onClause().longName(1).NAME(1).getText();

        return new Queries.JoinTableQuery(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol);
    }

    private static Constraints getConstraints(SQLParser.ConstraintContext currConstraint) {
        Constraints constraint;
        String text = currConstraint.getText();
        constraint = switch (text) {
        case "NOTNULL" -> Constraints.NOT_NULL;
        case "PRIMARYKEY" -> Constraints.PRIMARY_KEY;
        case "AUTOINCREMENT" -> Constraints.AUTOINCREMENT;
        case "UNIQUE" -> Constraints.UNIQUE;
        case "CHECK" -> Constraints.CHECK;
        case  "DEFAULT" -> Constraints.DEFAULT;
        default ->  null;
        };
        return constraint;
    }

    private ColumnMetadata parseColumn(SQLParser.ColumnContext columnContext) {
        DataType dataType = switch (columnContext.TYPE().getText()) {
            case "INTEGER" -> DataType.INTEGER;
            case "REAL" -> DataType.REAL;
            case "TEXT" -> DataType.TEXT;
            case "NULL" -> DataType.NULL;
            default -> null;
        };

        ArrayList<Constraints> constraints = new ArrayList<>();
        for (SQLParser.ConstraintContext constraintContext : columnContext.constraint()) {
            Constraints constraint = getConstraints(constraintContext);
            if (constraint != null) {
                constraints.add(constraint);
            }
        }

        Collate collate = null; // TODO
        return new ColumnMetadata(columnContext.name().getText(), dataType, 0, constraints, collate);
    }

}
