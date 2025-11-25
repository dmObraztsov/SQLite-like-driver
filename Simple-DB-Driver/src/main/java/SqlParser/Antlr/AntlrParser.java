package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

import java.util.ArrayList;
import java.util.HashMap;

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
    public QueryInterface visitAlterTableStatement(SQLParser.AlterTableStatementContext ctx) {
        String tableName = ctx.name().getText();
        SQLParser.AlterActionContext alterAction = ctx.alterAction();

        if (alterAction.renameTable() != null) {
            // Обработка RENAME TABLE
            String newTableName = alterAction.renameTable().name().getText();
            return new Queries.AlterTableQuery.AlterRenameTableQuery(tableName, newTableName);
        }
        else if (alterAction.addColumn() != null) {
            // Обработка ADD COLUMN
            SQLParser.ColumnContext columnContext = alterAction.addColumn().column();
            ColumnMetadata column = parseColumn(columnContext);
            return new Queries.AlterTableQuery.AlterAddColumnQuery(tableName, column);
        }
        else if (alterAction.dropColumn() != null) {
            // Обработка DROP COLUMN
            String columnName = alterAction.dropColumn().name().getText();
            return new Queries.AlterTableQuery.AlterDropColumnQuery(tableName, columnName);
        }
        else if (alterAction.renameColumn() != null) {
            // Обработка RENAME COLUMN
            String oldColumnName = alterAction.renameColumn().name(0).getText();
            String newColumnName = alterAction.renameColumn().name(1).getText();
            return new Queries.AlterTableQuery.AlterRenameColumnQuery(tableName, oldColumnName, newColumnName);
        }

        return new Queries.AlterTableQuery(tableName);
    }

    @Override
    public QueryInterface visitInsertTableStatement(SQLParser.InsertTableStatementContext ctx) {
        String tableName = ctx.tablename().getText();
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        for(int i = 0; i < ctx.name().size(); i++)
        {
            columns.add(ctx.name().get(i).getText());
        }

        for(int i = 0; i < ctx.data().size(); i++)
        {
            values.add(ctx.data().get(i).getText());
        }

        return new Queries.InsertTableQuery(tableName, columns, values);
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

    private ColumnMetadata parseColumn(SQLParser.ColumnContext columnContext) {
        DataType dataType = switch (columnContext.TYPE().getText()) {
            case "INTEGER" -> DataType.INTEGER;
            case "REAL" -> DataType.REAL;
            case "TEXT" -> DataType.TEXT;
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
        return new ColumnMetadata(columnContext.name().getText(), dataType, 0, constraints, collate, 0, null, null);
    }

}
