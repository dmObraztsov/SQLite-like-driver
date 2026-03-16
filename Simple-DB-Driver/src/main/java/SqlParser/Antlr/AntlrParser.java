package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

import java.util.ArrayList;
import java.util.List;

public class AntlrParser extends SQLBaseVisitor<QueryInterface> {

    @Override
    public QueryInterface visitQuery(SQLParser.QueryContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public QueryInterface visitCreateDBStatement(SQLParser.CreateDBStatementContext ctx) {
        return new Queries.CreateDataBaseQuery(ctx.identifier().getText());
    }

    @Override
    public QueryInterface visitDropDBStatement(SQLParser.DropDBStatementContext ctx) {
        return new Queries.DropDataBaseQuery(ctx.identifier().getText());
    }

    @Override
    public QueryInterface visitUseDBStatement(SQLParser.UseDBStatementContext ctx) {
        return new Queries.UseDataBaseQuery(ctx.identifier().getText());
    }

    @Override
    public QueryInterface visitCreateTableStatement(SQLParser.CreateTableStatementContext ctx) {
        String tableName = ctx.identifier().getText();
        ArrayList<ColumnMetadata> columns = new ArrayList<>();
        for (SQLParser.ColumnDefContext columnDefContext : ctx.columnDef()) {
            columns.add(parseColumn(columnDefContext));
        }
        return new Queries.CreateTableQuery(tableName, columns);
    }

    @Override
    public QueryInterface visitDropTableStatement(SQLParser.DropTableStatementContext ctx) {
        return new Queries.DropTableQuery(ctx.identifier().getText());
    }

    @Override
    public QueryInterface visitInsertTableStatement(SQLParser.InsertTableStatementContext ctx) {
        String tableName = ctx.identifier(0).getText();
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        for (int i = 1; i < ctx.identifier().size(); i++) {
            columns.add(ctx.identifier(i).getText());
        }

        for (SQLParser.LiteralContext literalContext : ctx.literal()) {
            values.add(literalContext.getText());
        }

        return new Queries.InsertTableQuery(tableName, columns, values);
    }

    @Override
    public QueryInterface visitSelectStatement(SQLParser.SelectStatementContext ctx) {
        String baseTable = ctx.tablename().getText();

        if (ctx.joinClause().isEmpty()) {
            boolean isStar = ctx.selectCols().STAR() != null;
            List<String> columns = null;
            if (!isStar) {
                columns = ctx.selectCols().columnRef().stream()
                        .map(this::toUnqualifiedColumnName)
                        .toList();
            }

            SimplePredicate where = extractSimpleWhere(ctx.whereClause());
            return new Queries.SelectDataQuery(
                    columns,
                    isStar,
                    baseTable,
                    where == null ? null : where.columnName(),
                    where == null ? null : where.literalValue()
            );
        }

        if (ctx.joinClause().size() != 1) {
            throw new IllegalArgumentException("Only one JOIN is supported right now");
        }
        if (ctx.whereClause() != null) {
            throw new IllegalArgumentException("JOIN with WHERE is not supported right now");
        }

        SQLParser.JoinClauseContext joinClause = ctx.joinClause(0);
        SimpleJoin join = extractSimpleJoin(joinClause);

        List<String> leftColumns = new ArrayList<>();
        List<String> rightColumns = new ArrayList<>();

        if (ctx.selectCols().STAR() == null) {
            for (SQLParser.ColumnRefContext columnRef : ctx.selectCols().columnRef()) {
                if (columnRef.identifier().size() != 2) {
                    throw new IllegalArgumentException("JOIN projection must use qualified names: table.column");
                }

                String tableName = columnRef.identifier(0).getText();
                String columnName = columnRef.identifier(1).getText();

                if (tableName.equals(baseTable)) {
                    leftColumns.add(columnName);
                } else if (tableName.equals(join.rightTableName())) {
                    rightColumns.add(columnName);
                } else {
                    throw new IllegalArgumentException("Unknown table in SELECT list: " + tableName);
                }
            }
        }

        return new Queries.JoinTableQuery(
                baseTable,
                leftColumns.isEmpty() ? null : leftColumns,
                join.rightTableName(),
                rightColumns.isEmpty() ? null : rightColumns,
                join.leftColumnName(),
                join.rightColumnName()
        );
    }

    @Override
    public QueryInterface visitAlterTableStatement(SQLParser.AlterTableStatementContext ctx) {
        String tableName = ctx.identifier().getText();
        SQLParser.AlterActionContext actionCtx = ctx.alterAction();

        if (actionCtx.columnDef() != null) {
            return new Queries.AlterTableAddColumnQuery(tableName, parseColumn(actionCtx.columnDef()));
        }

        if (actionCtx.DROP() != null && actionCtx.COLUMN() != null && actionCtx.identifier().size() >= 2) {
            return new Queries.AlterTableDropColumnQuery(tableName, actionCtx.identifier(0).getText());
        }

        throw new IllegalArgumentException("Unsupported ALTER TABLE statement");
    }

    @Override
    public QueryInterface visitBeginTransactionStatement(SQLParser.BeginTransactionStatementContext ctx) {
        return new Queries.BeginTransactionQuery();
    }

    @Override
    public QueryInterface visitCommitStatement(SQLParser.CommitStatementContext ctx) {
        return new Queries.CommitQuery();
    }

    @Override
    public QueryInterface visitRollbackStatement(SQLParser.RollbackStatementContext ctx) {
        return new Queries.RollbackQuery();
    }

    private ColumnMetadata parseColumn(SQLParser.ColumnDefContext columnContext) {
        DataType dataType = parseDataType(columnContext.dataType());
        ArrayList<Constraints> constraints = new ArrayList<>();
        String defaultValue = null;
        String checkExpression = null;

        for (SQLParser.ColumnConstraintContext constraintContext : columnContext.columnConstraint()) {
            if (constraintContext.notNullConstraint() != null) {
                constraints.add(Constraints.NOT_NULL);
            } else if (constraintContext.primaryKeyConstraint() != null) {
                constraints.add(Constraints.PRIMARY_KEY);
            } else if (constraintContext.autoIncrementConstraint() != null) {
                constraints.add(Constraints.AUTOINCREMENT);
            } else if (constraintContext.uniqueConstraint() != null) {
                constraints.add(Constraints.UNIQUE);
            } else if (constraintContext.nullConstraint() != null) {
                // explicit NULL is accepted but does not add a storage constraint
            } else if (constraintContext.checkConstraint() != null) {
                constraints.add(Constraints.CHECK);
                checkExpression = constraintContext.checkConstraint().condition().getText();
            } else if (constraintContext.defaultConstraint() != null) {
                constraints.add(Constraints.DEFAULT);
                defaultValue = constraintContext.defaultConstraint().literal().getText();
            }
        }

        ColumnMetadata metadata = new ColumnMetadata(
                columnContext.identifier().getText(),
                dataType,
                0,
                constraints,
                (Collate) null
        );
        metadata.setDefaultValue(defaultValue);
        metadata.setCheckExpression(checkExpression);
        return metadata;
    }

    private DataType parseDataType(SQLParser.DataTypeContext ctx) {
        if (ctx.INTEGER() != null) return DataType.INTEGER;
        if (ctx.REAL() != null) return DataType.REAL;
        if (ctx.TEXT() != null) return DataType.TEXT;
        if (ctx.BLOB() != null) return DataType.BLOB;
        throw new IllegalArgumentException("Unsupported data type: " + ctx.getText());
    }

    private String toUnqualifiedColumnName(SQLParser.ColumnRefContext columnRef) {
        if (columnRef.identifier().size() == 1) {
            return columnRef.identifier(0).getText();
        }
        return columnRef.identifier(1).getText();
    }

    private SimplePredicate extractSimpleWhere(SQLParser.WhereClauseContext whereClause) {
        if (whereClause == null) {
            return null;
        }

        SQLParser.ConditionContext condition = whereClause.condition();
        if (condition.orCondition().andCondition().size() != 1) {
            throw new IllegalArgumentException("OR in WHERE is not supported right now");
        }

        SQLParser.AndConditionContext andCondition = condition.orCondition().andCondition(0);
        if (andCondition.predicate().size() != 1) {
            throw new IllegalArgumentException("AND in WHERE is not supported right now");
        }

        SQLParser.PredicateContext predicate = andCondition.predicate(0);
        if (predicate.comparisonOperator() == null || predicate.comparisonOperator().EQ() == null) {
            throw new IllegalArgumentException("Only WHERE column = literal is supported right now");
        }
        if (predicate.operand(0).columnRef() == null || predicate.operand(1).literal() == null) {
            throw new IllegalArgumentException("Only WHERE column = literal is supported right now");
        }

        return new SimplePredicate(
                toUnqualifiedColumnName(predicate.operand(0).columnRef()),
                predicate.operand(1).literal().getText()
        );
    }

    private SimpleJoin extractSimpleJoin(SQLParser.JoinClauseContext joinClause) {
        SQLParser.ConditionContext condition = joinClause.condition();
        if (condition.orCondition().andCondition().size() != 1) {
            throw new IllegalArgumentException("Complex JOIN conditions are not supported right now");
        }

        SQLParser.AndConditionContext andCondition = condition.orCondition().andCondition(0);
        if (andCondition.predicate().size() != 1) {
            throw new IllegalArgumentException("Complex JOIN conditions are not supported right now");
        }

        SQLParser.PredicateContext predicate = andCondition.predicate(0);
        if (predicate.comparisonOperator() == null || predicate.comparisonOperator().EQ() == null) {
            throw new IllegalArgumentException("Only JOIN ... ON left = right is supported right now");
        }
        if (predicate.operand(0).columnRef() == null || predicate.operand(1).columnRef() == null) {
            throw new IllegalArgumentException("JOIN operands must be columns");
        }

        SQLParser.ColumnRefContext leftRef = predicate.operand(0).columnRef();
        SQLParser.ColumnRefContext rightRef = predicate.operand(1).columnRef();
        if (leftRef.identifier().size() != 2 || rightRef.identifier().size() != 2) {
            throw new IllegalArgumentException("JOIN columns must be qualified: table.column");
        }

        return new SimpleJoin(
                joinClause.tablename().getText(),
                leftRef.identifier(1).getText(),
                rightRef.identifier(1).getText()
        );
    }

    private record SimplePredicate(String columnName, String literalValue) {}

    private record SimpleJoin(String rightTableName, String leftColumnName, String rightColumnName) {}
}
