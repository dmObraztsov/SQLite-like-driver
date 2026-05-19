package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import SqlParser.QueriesStruct.WhereCondition;
import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            columns.add(parseColumn2(columnDefContext));
        }

        List<String> compositePK = null;
        if (ctx.tablePkConstraint() != null) {
            compositePK = ctx.tablePkConstraint().identifier().stream()
                    .map(SQLParser.IdentifierContext::getText)
                    .toList();
        }

        return new Queries.CreateTableQuery(tableName, columns, compositePK);
    }

    @Override
    public QueryInterface visitDropTableStatement(SQLParser.DropTableStatementContext ctx) {
        boolean ifExists = ctx.IF() != null;
        return new Queries.DropTableQuery(ctx.identifier().getText(), ifExists);
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
            values.add(getCleanLiteral(literalContext));
        }

        return new Queries.InsertTableQuery(tableName, columns, values);
    }

    @Override
    public QueryInterface visitSelectStatement(SQLParser.SelectStatementContext ctx) {
        String baseTable = ctx.tablename().getText();
        boolean isDistinct = ctx.DISTINCT() != null;

        List<SQLParser.SelectColContext> selectColCtxs = ctx.selectCols().selectCol();
        boolean isStar = ctx.selectCols().STAR() != null;

        List<SQLParser.AggregateFuncContext> aggFuncs = selectColCtxs.stream()
                .filter(sc -> sc.aggregateFunc() != null)
                .map(SQLParser.SelectColContext::aggregateFunc)
                .toList();

        List<SQLParser.ColumnRefContext> colRefs = selectColCtxs.stream()
                .filter(sc -> sc.columnRef() != null)
                .map(SQLParser.SelectColContext::columnRef)
                .toList();

        WhereCondition where = extractWhere(ctx.whereClause());
        boolean isJoin = !ctx.joinClause().isEmpty();

        // Build ORDER BY list (qualified for JOIN context to match engine row keys)
        List<Queries.OrderByItem> orderBy = new ArrayList<>();
        if (ctx.orderByClause() != null) {
            for (SQLParser.OrderByItemContext item : ctx.orderByClause().orderByItem()) {
                String col = isJoin ? item.columnRef().getText() : toUnqualifiedColumnName(item.columnRef());
                boolean asc = item.DESC() == null;
                orderBy.add(new Queries.OrderByItem(col, asc));
            }
        }

        // Build alias map: original key → alias name
        // For JOIN: use full qualified ref as key (e.g. "visits.id")
        // For non-JOIN: use unqualified ref as key (e.g. "id")
        Map<String, String> aliases = new LinkedHashMap<>();
        for (SQLParser.SelectColContext sc : selectColCtxs) {
            if (sc.alias() != null) {
                String key;
                if (sc.columnRef() != null) {
                    key = isJoin ? sc.columnRef().getText() : toUnqualifiedColumnName(sc.columnRef());
                } else {
                    key = sc.aggregateFunc().funcName().getText().toUpperCase();
                }
                aliases.put(key, sc.alias().identifier().getText());
            }
        }

        int limit = -1;
        int offset = 0;
        if (ctx.limitClause() != null) {
            limit = Integer.parseInt(ctx.limitClause().NUMBER(0).getText());
            if (ctx.limitClause().NUMBER().size() > 1) {
                offset = Integer.parseInt(ctx.limitClause().NUMBER(1).getText());
            }
        }

        String groupByCol = null;
        WhereCondition having = null;
        if (ctx.groupByClause() != null) {
            groupByCol = toUnqualifiedColumnName(ctx.groupByClause().columnRef());
            if (ctx.groupByClause().condition() != null) {
                having = parseCondition(ctx.groupByClause().condition());
            }
        }

        if (groupByCol != null) {
            if (aggFuncs.isEmpty()) {
                throw new IllegalArgumentException("GROUP BY requires an aggregate function in SELECT");
            }
            if (aggFuncs.size() > 1) {
                throw new IllegalArgumentException("Only one aggregate function is supported with GROUP BY");
            }
            SQLParser.AggregateFuncContext aggCtx = aggFuncs.get(0);
            String funcName = aggCtx.funcName().getText().toUpperCase();
            String aggCol = aggCtx.columnRef() != null ? toUnqualifiedColumnName(aggCtx.columnRef()) : null;

            List<String> extraCols = colRefs.stream()
                    .map(this::toUnqualifiedColumnName)
                    .toList();

            return new Queries.GroupBySelectQuery(
                    baseTable, groupByCol, funcName, aggCol,
                    where, having, extraCols, orderBy, limit, offset
            );
        }

        if (!aggFuncs.isEmpty()) {
            if (aggFuncs.size() > 1) {
                throw new IllegalArgumentException("Only one aggregate function is supported at a time");
            }
            SQLParser.AggregateFuncContext aggCtx = aggFuncs.get(0);
            String funcName = aggCtx.funcName().getText().toUpperCase();
            String colName = aggCtx.columnRef() != null ? toUnqualifiedColumnName(aggCtx.columnRef()) : null;

            return switch (funcName) {
                case "COUNT" -> new Queries.CountQuery(baseTable, colName, where);
                case "SUM"   -> new Queries.SumQuery(baseTable, colName, where);
                case "AVG"   -> new Queries.AvgQuery(baseTable, colName, where);
                case "MIN"   -> new Queries.MinQuery(baseTable, colName, where);
                case "MAX"   -> new Queries.MaxQuery(baseTable, colName, where);
                default -> throw new IllegalArgumentException("Unsupported aggregate function: " + funcName);
            };
        }

        if (isJoin) {
            if (ctx.joinClause().size() != 1) {
                throw new IllegalArgumentException("Only one JOIN is supported right now");
            }

            SQLParser.JoinClauseContext joinClauseCtx = ctx.joinClause(0);
            SimpleJoin join = extractSimpleJoin(joinClauseCtx);
            boolean isLeftJoin = joinClauseCtx.joinType() != null
                    && joinClauseCtx.joinType().LEFT() != null;

            List<String> leftColumns = new ArrayList<>();
            List<String> rightColumns = new ArrayList<>();

            if (!isStar) {
                for (SQLParser.ColumnRefContext columnRef : colRefs) {
                    if (columnRef.identifier().size() == 2) {
                        String tbl  = columnRef.identifier(0).getText();
                        String col  = columnRef.identifier(1).getText();
                        if (tbl.equals(baseTable)) {
                            leftColumns.add(col);
                        } else if (tbl.equals(join.rightTableName())) {
                            rightColumns.add(col);
                        } else {
                            throw new IllegalArgumentException("Unknown table in SELECT list: " + tbl);
                        }
                    } else {
                        // unqualified column — add to both; projection will pick whichever table has it
                        String col = columnRef.identifier(0).getText();
                        leftColumns.add(col);
                        rightColumns.add(col);
                    }
                }
            }

            return new Queries.JoinTableQuery(
                    baseTable, leftColumns.isEmpty() ? null : leftColumns,
                    join.rightTableName(), rightColumns.isEmpty() ? null : rightColumns,
                    join.leftColumnName(), join.rightColumnName(), isDistinct, where,
                    isLeftJoin, orderBy, aliases, limit, offset
            );
        }

        List<String> columns = isStar ? null : colRefs.stream()
                .map(this::toUnqualifiedColumnName)
                .toList();

        return new Queries.SelectDataQuery(
                columns, isStar, baseTable, where, isDistinct, orderBy, aliases, limit, offset
        );
    }

    @Override
    public QueryInterface visitAlterTableStatement(SQLParser.AlterTableStatementContext ctx) {
        String tableName = ctx.name().getText();
        SQLParser.AlterActionContext actionCtx = ctx.alterAction();

        if (actionCtx.addColumn() != null) {
            ColumnMetadata column = parseColumn1(actionCtx.addColumn().column());
            return new Queries.AlterTableAddColumnQuery(tableName, column);
        } else if (actionCtx.dropColumn() != null) {
            String columnName = actionCtx.dropColumn().name().getText();
            return new Queries.AlterTableDropColumnQuery(tableName, columnName);
        } else if (actionCtx.renameColumn() != null) {
            String oldName = actionCtx.renameColumn().name(0).getText();
            String newName = actionCtx.renameColumn().name(1).getText();
            return new Queries.AlterTableRenameColumnQuery(tableName, oldName, newName);
        } else if (actionCtx.renameTable() != null) {
            String newName = actionCtx.renameTable().name().getText();
            return new Queries.AlterTableRenameTableQuery(tableName, newName);
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

    @Override
    public QueryInterface visitDeleteStatement(SQLParser.DeleteStatementContext ctx) {
        String tableName = ctx.tablename().getText();
        WhereCondition where = extractWhere(ctx.whereClause());
        return new Queries.DeleteTableQuery(tableName, where);
    }

    @Override
    public QueryInterface visitUpdateStatement(SQLParser.UpdateStatementContext ctx) {
        String tableName = ctx.tablename().getText();
        Map<String, String> setValues = new HashMap<>();

        for (SQLParser.UpdateAssignmentContext assignCtx : ctx.updateAssignment()) {
            String colName = assignCtx.columnRef().identifier().get(0).getText();
            SQLParser.OperandContext op = assignCtx.operand();
            String value;
            if (op.literal() != null) {
                value = getCleanLiteral(op.literal());
            } else if (op.columnRef() != null) {
                value = op.columnRef().getText();
            } else {
                throw new IllegalArgumentException("Unsupported operand in UPDATE");
            }
            setValues.put(colName, value);
        }

        WhereCondition where = extractWhere(ctx.whereClause());
        return new Queries.UpdateTableQuery(tableName, setValues, where);
    }

    private WhereCondition extractWhere(SQLParser.WhereClauseContext whereClause) {
        if (whereClause == null) return null;
        return parseCondition(whereClause.condition());
    }

    private WhereCondition parseCondition(SQLParser.ConditionContext ctx) {
        List<SQLParser.AndConditionContext> andClauses = ctx.orCondition().andCondition();
        if (andClauses.size() == 1) {
            return parseAndCondition(andClauses.get(0));
        }
        List<WhereCondition> operands = andClauses.stream()
                .map(this::parseAndCondition)
                .toList();
        return new WhereCondition.Or(operands);
    }

    private WhereCondition parseAndCondition(SQLParser.AndConditionContext ctx) {
        List<SQLParser.PredicateContext> predicates = ctx.predicate();
        if (predicates.size() == 1) {
            return parsePredicate(predicates.get(0));
        }
        List<WhereCondition> operands = predicates.stream()
                .map(this::parsePredicate)
                .toList();
        return new WhereCondition.And(operands);
    }

    private WhereCondition parsePredicate(SQLParser.PredicateContext ctx) {
        if (ctx.condition() != null) {
            return parseCondition(ctx.condition());
        }

        if (ctx.IS() != null) {
            String col = toUnqualifiedColumnName(ctx.columnRef());
            boolean isNull = ctx.NOT() == null;
            return new WhereCondition.IsNull(col, isNull);
        }

        if (ctx.IN() != null) {
            String col = toUnqualifiedColumnName(ctx.columnRef());
            boolean negated = ctx.NOT() != null;
            List<String> vals = ctx.literal().stream()
                    .map(this::getCleanLiteral)
                    .collect(java.util.stream.Collectors.toList());
            return new WhereCondition.In(col, vals, negated);
        }

        if (ctx.BETWEEN() != null) {
            String col = toUnqualifiedColumnName(ctx.columnRef());
            String low  = getOperandValue(ctx.operand(0));
            String high = getOperandValue(ctx.operand(1));
            return new WhereCondition.Between(col, low, high);
        }

        if (ctx.LIKE() != null) {
            SQLParser.OperandContext left = ctx.operand(0);
            SQLParser.OperandContext right = ctx.operand(1);
            String columnName = left.columnRef() != null
                    ? toUnqualifiedColumnName(left.columnRef())
                    : left.getText();
            String pattern = right.literal() != null
                    ? getCleanLiteral(right.literal())
                    : right.getText();
            return new WhereCondition.Simple(columnName, "LIKE", pattern);
        }

        SQLParser.OperandContext left = ctx.operand(0);
        SQLParser.OperandContext right = ctx.operand(1);
        String op = extractOperator(ctx.comparisonOperator());

        String columnName;
        if (left.columnRef() != null) {
            columnName = toUnqualifiedColumnName(left.columnRef());
        } else if (left.aggregateFunc() != null) {
            columnName = left.aggregateFunc().funcName().getText().toUpperCase();
        } else {
            throw new IllegalArgumentException("Left side of predicate must be a column reference");
        }

        String value;
        if (right.literal() != null) {
            value = getCleanLiteral(right.literal());
        } else if (right.columnRef() != null) {
            value = toUnqualifiedColumnName(right.columnRef());
        } else if (right.aggregateFunc() != null) {
            value = right.aggregateFunc().funcName().getText().toUpperCase();
        } else {
            throw new IllegalArgumentException("Right side of predicate must be a literal or column reference");
        }

        return new WhereCondition.Simple(columnName, op, value);
    }

    private String getOperandValue(SQLParser.OperandContext op) {
        if (op.literal() != null) return getCleanLiteral(op.literal());
        if (op.columnRef() != null) return toUnqualifiedColumnName(op.columnRef());
        return op.getText();
    }

    private static Constraints getConstraints(SQLParser.ConstraintContext currConstraint) {
        String text = currConstraint.getText();
        return switch (text) {
            case "NOTNULL"       -> Constraints.NOT_NULL;
            case "PRIMARYKEY"    -> Constraints.PRIMARY_KEY;
            case "AUTOINCREMENT" -> Constraints.AUTOINCREMENT;
            case "UNIQUE"        -> Constraints.UNIQUE;
            case "CHECK"         -> Constraints.CHECK;
            case "DEFAULT"       -> Constraints.DEFAULT;
            default              -> null;
        };
    }

    private ColumnMetadata parseColumn1(SQLParser.ColumnContext columnContext) {
        DataType dataType = switch (columnContext.dataType().getText().toUpperCase()) {
            case "INTEGER" -> DataType.INTEGER;
            case "REAL"    -> DataType.REAL;
            case "TEXT"    -> DataType.TEXT;
            case "NULL"    -> DataType.NULL;
            default        -> null;
        };

        return new ColumnMetadata(
                columnContext.name().getText(),
                dataType,
                0,
                new ArrayList<>(),
                null
        );
    }

    private ColumnMetadata parseColumn2(SQLParser.ColumnDefContext columnContext) {
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
                // explicit NULL — no constraint stored
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
        if (ctx.INTEGER() != null)  return DataType.INTEGER;
        if (ctx.REAL() != null)     return DataType.REAL;
        if (ctx.TEXT() != null)     return DataType.TEXT;
        if (ctx.BLOB() != null)     return DataType.BLOB;
        if (ctx.VARCHAR() != null)  return DataType.TEXT;
        if (ctx.DATE() != null)     return DataType.TEXT;
        if (ctx.DATETIME() != null) return DataType.TEXT;
        throw new IllegalArgumentException("Unsupported data type: " + ctx.getText());
    }

    private String toUnqualifiedColumnName(SQLParser.ColumnRefContext columnRef) {
        if (columnRef.identifier().size() == 1) {
            return columnRef.identifier(0).getText();
        }
        return columnRef.identifier(1).getText();
    }

    private String extractOperator(SQLParser.ComparisonOperatorContext ctx) {
        if (ctx.EQ() != null) return "=";
        if (ctx.NE() != null) return "!=";
        if (ctx.GT() != null) return ">";
        if (ctx.LT() != null) return "<";
        if (ctx.GE() != null) return ">=";
        if (ctx.LE() != null) return "<=";
        return "=";
    }

    private SimpleJoin extractSimpleJoin(SQLParser.JoinClauseContext joinClause) {
        SQLParser.ConditionContext condition = joinClause.condition();
        List<SQLParser.AndConditionContext> andClauses = condition.orCondition().andCondition();
        if (andClauses.size() != 1) {
            throw new IllegalArgumentException("Complex JOIN conditions are not supported right now");
        }
        SQLParser.AndConditionContext andCondition = andClauses.get(0);
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

        SQLParser.ColumnRefContext leftRef  = predicate.operand(0).columnRef();
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

    private record SimpleJoin(String rightTableName, String leftColumnName, String rightColumnName) {}

    private String getCleanLiteral(SQLParser.LiteralContext ctx) {
        if (ctx == null) return null;
        if (ctx.STRING() != null) {
            String text = ctx.STRING().getText();
            if (text.length() >= 2 && (text.startsWith("\"") || text.startsWith("'"))) {
                return text.substring(1, text.length() - 1);
            }
            return text;
        }
        if (ctx.NULL() != null) return null;
        return ctx.getText();
    }
}
