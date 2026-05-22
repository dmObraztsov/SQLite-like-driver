package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import SqlParser.QueriesStruct.WhereCondition;
import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

import java.util.ArrayList;
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
        boolean ifNotExists = ctx.ifNotExists() != null;
        return new Queries.CreateDataBaseQuery(ctx.identifier().getText(), ifNotExists);
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

        String baseTable = ctx.tablename().identifier(0).getText();
        String baseAlias = ctx.tablename().identifier().size() > 1
                ? ctx.tablename().identifier(1).getText() : null;

        boolean isDistinct = ctx.DISTINCT() != null;

        Map<String, String> aliasMap = new LinkedHashMap<>();
        aliasMap.put(baseAlias != null ? baseAlias : baseTable, baseTable);

        List<SQLParser.JoinClauseContext> joinCtxs = ctx.joinClause();
        boolean hasJoins = !joinCtxs.isEmpty();

        List<Queries.JoinSpec> joinSpecs = new ArrayList<>();
        for (SQLParser.JoinClauseContext jc : joinCtxs) {
            String jtName  = jc.tablename().identifier(0).getText();
            String jtAlias = jc.tablename().identifier().size() > 1
                    ? jc.tablename().identifier(1).getText() : null;
            aliasMap.put(jtAlias != null ? jtAlias : jtName, jtName);

            boolean isLeft = jc.joinType() != null && jc.joinType().LEFT() != null;
            String[] pair = extractJoinKeyPair(jc.condition(), aliasMap);
            joinSpecs.add(new Queries.JoinSpec(jtName, jtAlias, pair[0], pair[1], isLeft));
        }

        List<SQLParser.SelectColContext> selectColCtxs = ctx.selectCols().selectCol();
        boolean isStar = ctx.selectCols().STAR() != null;

        List<Queries.AggSpec> aggSpecs = new ArrayList<>();
        List<String> regularCols = new ArrayList<>();
        Map<String, String> colAliases = new LinkedHashMap<>();

        if (!isStar) {
            for (SQLParser.SelectColContext sc : selectColCtxs) {
                String alias = sc.alias() != null ? sc.alias().identifier().getText() : null;
                if (sc.aggregateFunc() != null) {
                    String func = sc.aggregateFunc().funcName().getText().toUpperCase();
                    String col = sc.aggregateFunc().columnRef() != null
                            ? sc.aggregateFunc().columnRef().identifier()
                               .get(sc.aggregateFunc().columnRef().identifier().size() - 1).getText()
                            : null;
                    String aggAlias = alias != null ? alias : func;
                    aggSpecs.add(new Queries.AggSpec(func, col, aggAlias));
                } else {
                    String colFull = resolveColRef(sc.columnRef(), aliasMap);
                    regularCols.add(colFull);
                    if (alias != null) colAliases.put(colFull, alias);
                }
            }
        }

        WhereCondition where = extractWhere(ctx.whereClause());
        if (where != null && (hasJoins || baseAlias != null)) {
            where = normalizeWhere(where, aliasMap);
        }

        List<String> groupByCols = new ArrayList<>();
        WhereCondition having = null;
        if (ctx.groupByClause() != null) {
            for (SQLParser.ColumnRefContext cref : ctx.groupByClause().columnRef()) {
                groupByCols.add(resolveColRef(cref, aliasMap));
            }
            if (ctx.groupByClause().condition() != null) {
                having = parseCondition(ctx.groupByClause().condition());
            }
        }

        List<Queries.OrderByItem> orderBy = new ArrayList<>();
        if (ctx.orderByClause() != null) {
            for (SQLParser.OrderByItemContext item : ctx.orderByClause().orderByItem()) {
                String col = resolveColRef(item.columnRef(), aliasMap);
                boolean asc = item.DESC() == null;
                orderBy.add(new Queries.OrderByItem(col, asc));
            }
        }

        int limit = -1, offset = 0;
        if (ctx.limitClause() != null) {
            limit = Integer.parseInt(ctx.limitClause().NUMBER(0).getText());
            if (ctx.limitClause().NUMBER().size() > 1) {
                offset = Integer.parseInt(ctx.limitClause().NUMBER(1).getText());
            }
        }

        boolean needsAdvanced = hasJoins
                || baseAlias != null
                || aggSpecs.size() > 1
                || groupByCols.size() > 1;

        if (needsAdvanced) {
            return new Queries.AdvancedSelectQuery(
                    baseTable, baseAlias, joinSpecs, isStar,
                    regularCols, colAliases, aggSpecs,
                    groupByCols, where, having,
                    isDistinct, orderBy, limit, offset
            );
        }

        if (!groupByCols.isEmpty() && !aggSpecs.isEmpty()) {

            String groupByCol = groupByCols.get(0);
            Queries.AggSpec agg = aggSpecs.get(0);
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put(agg.func(), agg.alias());
            for (String col : regularCols) {
                if (colAliases.containsKey(col)) aliases.put(col, colAliases.get(col));
            }
            return new Queries.GroupBySelectQuery(
                    baseTable, groupByCol, agg.func(), agg.col(),
                    where, having, regularCols, orderBy, limit, offset, aliases
            );
        }

        if (!aggSpecs.isEmpty()) {
            Queries.AggSpec agg = aggSpecs.get(0);
            String alias = agg.alias().equals(agg.func()) ? null : agg.alias();
            return switch (agg.func()) {
                case "COUNT" -> new Queries.CountQuery(baseTable, agg.col(), where, alias);
                case "SUM"   -> new Queries.SumQuery(baseTable, agg.col(), where, alias);
                case "AVG"   -> new Queries.AvgQuery(baseTable, agg.col(), where, alias);
                case "MIN"   -> new Queries.MinQuery(baseTable, agg.col(), where, alias);
                case "MAX"   -> new Queries.MaxQuery(baseTable, agg.col(), where, alias);
                default -> throw new IllegalArgumentException("Unknown agg: " + agg.func());
            };
        }

        List<String> columns = isStar ? null : new ArrayList<>(regularCols);
        Map<String, String> aliases = new LinkedHashMap<>(colAliases);
        return new Queries.SelectDataQuery(
                columns, isStar, baseTable, where, isDistinct, orderBy, aliases, limit, offset
        );
    }

    private String[] extractJoinKeyPair(SQLParser.ConditionContext cond, Map<String, String> aliasMap) {
        SQLParser.AndConditionContext andCond = cond.orCondition().andCondition(0);
        SQLParser.PredicateContext pred = andCond.predicate(0);

        SQLParser.ColumnRefContext leftRef  = pred.operand(0).columnRef();
        SQLParser.ColumnRefContext rightRef = pred.operand(1).columnRef();

        String leftKey = resolveColRef(leftRef, aliasMap);

        String rightKey = rightRef.identifier().get(rightRef.identifier().size() - 1).getText();

        return new String[]{leftKey, rightKey};
    }

    private String resolveColRef(SQLParser.ColumnRefContext cref, Map<String, String> aliasMap) {
        if (cref.identifier().size() == 2) {
            String tablePart = cref.identifier(0).getText();
            String colPart   = cref.identifier(1).getText();
            return tablePart + "." + colPart;
        }
        return cref.identifier(0).getText();
    }

    private WhereCondition normalizeWhere(WhereCondition cond, Map<String, String> aliasMap) {
        return switch (cond) {
            case WhereCondition.Simple s -> new WhereCondition.Simple(
                    normalizeCol(s.column(), aliasMap), s.op(), s.value()
            );
            case WhereCondition.And and -> new WhereCondition.And(
                    and.operands().stream().map(c -> normalizeWhere(c, aliasMap)).toList()
            );
            case WhereCondition.Or or -> new WhereCondition.Or(
                    or.operands().stream().map(c -> normalizeWhere(c, aliasMap)).toList()
            );
            case WhereCondition.IsNull n -> new WhereCondition.IsNull(
                    normalizeCol(n.column(), aliasMap), n.isNull()
            );
            case WhereCondition.In in -> new WhereCondition.In(
                    normalizeCol(in.column(), aliasMap), in.values(), in.negated()
            );
            case WhereCondition.Between b -> new WhereCondition.Between(
                    normalizeCol(b.column(), aliasMap), b.low(), b.high()
            );
        };
    }

    private String normalizeCol(String name, Map<String, String> aliasMap) {

        return name;
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
        String tableName = ctx.tablename().identifier(0).getText();
        WhereCondition where = extractWhere(ctx.whereClause());
        return new Queries.DeleteTableQuery(tableName, where);
    }

    @Override
    public QueryInterface visitUpdateStatement(SQLParser.UpdateStatementContext ctx) {
        String tableName = ctx.tablename().identifier(0).getText();
        Map<String, String> setValues = new java.util.HashMap<>();

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
            String col = toFullColumnName(ctx.columnRef());
            boolean isNull = ctx.NOT() == null;
            return new WhereCondition.IsNull(col, isNull);
        }

        if (ctx.IN() != null) {
            String col = toFullColumnName(ctx.columnRef());
            boolean negated = ctx.NOT() != null;
            List<String> vals = ctx.literal().stream()
                    .map(this::getCleanLiteral)
                    .collect(java.util.stream.Collectors.toList());
            return new WhereCondition.In(col, vals, negated);
        }

        if (ctx.BETWEEN() != null) {
            String col  = toFullColumnName(ctx.columnRef());
            String low  = getOperandValue(ctx.operand(0));
            String high = getOperandValue(ctx.operand(1));
            return new WhereCondition.Between(col, low, high);
        }

        if (ctx.LIKE() != null) {
            SQLParser.OperandContext left  = ctx.operand(0);
            SQLParser.OperandContext right = ctx.operand(1);
            String columnName = left.columnRef() != null
                    ? toFullColumnName(left.columnRef())
                    : left.getText();
            String pattern = right.literal() != null
                    ? getCleanLiteral(right.literal())
                    : right.getText();
            return new WhereCondition.Simple(columnName, "LIKE", pattern);
        }

        SQLParser.OperandContext left  = ctx.operand(0);
        SQLParser.OperandContext right = ctx.operand(1);
        String op = extractOperator(ctx.comparisonOperator());

        String columnName;
        if (left.columnRef() != null) {
            columnName = toFullColumnName(left.columnRef());
        } else if (left.aggregateFunc() != null) {
            columnName = left.aggregateFunc().funcName().getText().toUpperCase();
        } else {
            throw new IllegalArgumentException("Left side of predicate must be a column reference");
        }

        String value;
        if (right.literal() != null) {
            value = getCleanLiteral(right.literal());
        } else if (right.columnRef() != null) {
            value = toFullColumnName(right.columnRef());
        } else if (right.aggregateFunc() != null) {
            value = right.aggregateFunc().funcName().getText().toUpperCase();
        } else {
            throw new IllegalArgumentException("Right side of predicate must be a literal or column reference");
        }

        return new WhereCondition.Simple(columnName, op, value);
    }

    private String getOperandValue(SQLParser.OperandContext op) {
        if (op.literal() != null) return getCleanLiteral(op.literal());
        if (op.columnRef() != null) return toFullColumnName(op.columnRef());
        return op.getText();
    }

   
    private String toFullColumnName(SQLParser.ColumnRefContext columnRef) {
        if (columnRef.identifier().size() == 2) {
            return columnRef.identifier(0).getText() + "." + columnRef.identifier(1).getText();
        }
        return columnRef.identifier(0).getText();
    }

   
    private String toUnqualifiedColumnName(SQLParser.ColumnRefContext columnRef) {
        if (columnRef.identifier().size() == 1) {
            return columnRef.identifier(0).getText();
        }
        return columnRef.identifier(1).getText();
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
        return new ColumnMetadata(columnContext.name().getText(), dataType, 0, new ArrayList<>(), null);
    }

    private ColumnMetadata parseColumn2(SQLParser.ColumnDefContext columnContext) {
        DataType dataType = parseDataType(columnContext.dataType());
        ArrayList<Constraints> constraints = new ArrayList<>();
        String defaultValue   = null;
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

            } else if (constraintContext.checkConstraint() != null) {
                constraints.add(Constraints.CHECK);
                checkExpression = constraintContext.checkConstraint().condition().getText();
            } else if (constraintContext.defaultConstraint() != null) {
                constraints.add(Constraints.DEFAULT);
                defaultValue = constraintContext.defaultConstraint().literal().getText();
            }
        }

        ColumnMetadata metadata = new ColumnMetadata(
                columnContext.identifier().getText(), dataType, 0, constraints, (Collate) null
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

    private String extractOperator(SQLParser.ComparisonOperatorContext ctx) {
        if (ctx.EQ() != null) return "=";
        if (ctx.NE() != null) return "!=";
        if (ctx.GT() != null) return ">";
        if (ctx.LT() != null) return "<";
        if (ctx.GE() != null) return ">=";
        if (ctx.LE() != null) return "<=";
        return "=";
    }

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
