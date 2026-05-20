package SqlParser.QueriesStruct;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;

import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class Queries {

    public record OrderByItem(String col, boolean asc) {}

    /** Specification for one JOIN clause. */
    public record JoinSpec(
            String tableName,
            String alias,
            String leftKey,    // fully-qualified key in accumulated rows (e.g. "flight_passengers.document_number")
            String rightKey,   // bare column name in the right table (e.g. "document_number")
            boolean isLeftJoin
    ) {}

    /** Specification for one aggregate function in SELECT. */
    public record AggSpec(String func, String col, String alias) {}

    // =========================================================
    // DDL / transaction queries (unchanged)
    // =========================================================

    public record CreateDataBaseQuery(String databaseName, boolean ifNotExists) implements QueryInterface {
        public CreateDataBaseQuery(String databaseName) { this(databaseName, false); }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            try { engine.createDatabase(databaseName); }
            catch (Exception e) { if (!ifNotExists) throw e; }
            return new ExecutionResult(true, "Database '" + databaseName + "' created.");
        }
    }

    public record DropDataBaseQuery(String databaseName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.dropDatabase(databaseName);
            return new ExecutionResult(true, "Database '" + databaseName + "' dropped.");
        }
    }

    public record UseDataBaseQuery(String databaseName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws FileStorageException {
            engine.setCurrentDatabase(databaseName);
            return new ExecutionResult(true, "Switched to database '" + databaseName + "'.");
        }
    }

    public record CreateTableQuery(String tableName, List<ColumnMetadata> columns,
                                   List<String> compositePrimaryKey) implements QueryInterface {
        public CreateTableQuery(String tableName, List<ColumnMetadata> columns) {
            this(tableName, columns, null);
        }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.createTable(tableName, columns, compositePrimaryKey);
            return new ExecutionResult(true, "Table '" + tableName + "' created successfully.");
        }
    }

    public record DropTableQuery(String tableName, boolean ifExists) implements QueryInterface {
        public DropTableQuery(String tableName) { this(tableName, false); }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            try { engine.dropTable(tableName); }
            catch (Exception e) { if (!ifExists) throw e; }
            return new ExecutionResult(true, "Table '" + tableName + "' dropped successfully.");
        }
    }

    public record InsertTableQuery(String tableName, List<String> columnNames, List<String> values) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.insert(tableName, columnNames, values);
            return new ExecutionResult(true, "Row inserted into '" + tableName + "'.");
        }
    }

    // =========================================================
    // SELECT: simple (no aggregates, no joins)
    // =========================================================

    public record SelectDataQuery(List<String> selectCols, boolean isStar, String tableName,
                                  WhereCondition where, boolean isDistinct,
                                  List<OrderByItem> orderBy,
                                  Map<String, String> aliases,
                                  int limit, int offset) implements QueryInterface {

        public SelectDataQuery(List<String> selectCols, boolean isStar, String tableName,
                               WhereCondition where, boolean isDistinct,
                               String orderByCol, boolean orderByAsc) {
            this(selectCols, isStar, tableName, where, isDistinct,
                 orderByCol != null ? List.of(new OrderByItem(orderByCol, orderByAsc)) : List.of(),
                 Map.of(), -1, 0);
        }

        public SelectDataQuery(List<String> selectCols, boolean isStar, String tableName,
                               WhereCondition where, boolean isDistinct,
                               String orderByCol, boolean orderByAsc, int limit, int offset) {
            this(selectCols, isStar, tableName, where, isDistinct,
                 orderByCol != null ? List.of(new OrderByItem(orderByCol, orderByAsc)) : List.of(),
                 Map.of(), limit, offset);
        }

        public SelectDataQuery(List<String> selectCols, boolean isStar, String tableName,
                               WhereCondition where, boolean isDistinct,
                               List<OrderByItem> orderBy, int limit, int offset) {
            this(selectCols, isStar, tableName, where, isDistinct, orderBy, Map.of(), limit, offset);
        }

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> results = engine.select(tableName, selectCols, isStar, where, isDistinct, null, true);
            return new ExecutionResult(true, "Select completed.",
                    applyLimitOffset(multiSort(applyAliases(results, aliases), orderBy), limit, offset));
        }
    }

    // =========================================================
    // SELECT: GROUP BY (legacy single-column, single-aggregate path)
    // =========================================================

    public record GroupBySelectQuery(String tableName, String groupByCol,
                                     String aggFunc, String aggCol,
                                     WhereCondition where, WhereCondition having,
                                     List<String> extraSelectCols,
                                     List<OrderByItem> orderBy,
                                     int limit, int offset,
                                     Map<String, String> aliases) implements QueryInterface {

        public GroupBySelectQuery(String tableName, String groupByCol,
                                  String aggFunc, String aggCol,
                                  WhereCondition where, WhereCondition having,
                                  List<String> extraSelectCols,
                                  String orderByCol, boolean orderByAsc) {
            this(tableName, groupByCol, aggFunc, aggCol, where, having, extraSelectCols,
                 orderByCol != null ? List.of(new OrderByItem(orderByCol, orderByAsc)) : List.of(),
                 -1, 0, Map.of());
        }

        public GroupBySelectQuery(String tableName, String groupByCol,
                                  String aggFunc, String aggCol,
                                  WhereCondition where, WhereCondition having,
                                  List<String> extraSelectCols,
                                  String orderByCol, boolean orderByAsc, int limit, int offset) {
            this(tableName, groupByCol, aggFunc, aggCol, where, having, extraSelectCols,
                 orderByCol != null ? List.of(new OrderByItem(orderByCol, orderByAsc)) : List.of(),
                 limit, offset, Map.of());
        }

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> results = engine.groupBySelect(
                    tableName, groupByCol, aggFunc, aggCol,
                    where, having, extraSelectCols, null, true
            );
            List<Row> aliased = applyAliases(results, aliases);
            return new ExecutionResult(true, "Group by completed.",
                    applyLimitOffset(multiSort(aliased, orderBy), limit, offset));
        }
    }

    // =========================================================
    // SELECT: JOIN (legacy 2-table path kept for compatibility)
    // =========================================================

    public record JoinTableQuery(String table1Name, List<String> columns1,
                                  String table2Name, List<String> columns2,
                                  String leftJoinCol, String rightJoinCol,
                                  boolean isDistinct, WhereCondition where,
                                  boolean isLeftJoin,
                                  List<OrderByItem> orderBy,
                                  Map<String, String> aliases,
                                  int limit, int offset) implements QueryInterface {

        public JoinTableQuery(String table1Name, List<String> columns1,
                              String table2Name, List<String> columns2,
                              String leftJoinCol, String rightJoinCol,
                              boolean isDistinct, WhereCondition where) {
            this(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol,
                 isDistinct, where, false, List.of(), Map.of(), -1, 0);
        }

        public JoinTableQuery(String table1Name, List<String> columns1,
                              String table2Name, List<String> columns2,
                              String leftJoinCol, String rightJoinCol,
                              boolean isDistinct) {
            this(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol,
                 isDistinct, null, false, List.of(), Map.of(), -1, 0);
        }

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> rawJoinedRows = engine.join(table1Name, columns1, table2Name, columns2,
                    leftJoinCol, rightJoinCol, isLeftJoin);
            List<Row> filtered = (where == null) ? rawJoinedRows
                    : engine.applyWhereToJoinedRows(where, rawJoinedRows);

            List<Row> projectedRows = new ArrayList<>();
            for (Row row : filtered) {
                if (columns1 == null && columns2 == null) {
                    projectedRows.add(row); continue;
                }
                Map<String, String> filteredValues = new HashMap<>();
                if (columns1 != null) {
                    for (String col : columns1) {
                        String fullKey = table1Name + "." + col;
                        String value = row.get(fullKey);
                        if (value != null) {
                            filteredValues.put(fullKey, value);
                            if (!col.equalsIgnoreCase("id")) filteredValues.putIfAbsent(col, value);
                        }
                    }
                }
                if (columns2 != null) {
                    for (String col : columns2) {
                        String fullKey = table2Name + "." + col;
                        String value = row.get(fullKey);
                        if (value != null) {
                            filteredValues.put(fullKey, value);
                            if (!col.equalsIgnoreCase("id")) filteredValues.putIfAbsent(col, value);
                        }
                    }
                }
                projectedRows.add(new Row(filteredValues));
            }

            List<Row> sorted  = multiSort(projectedRows, orderBy);
            List<Row> aliased = applyAliases(sorted, aliases);
            List<Row> deduped = isDistinct ? new ArrayList<>(new LinkedHashSet<>(aliased)) : aliased;
            return new ExecutionResult(true, "Join completed", applyLimitOffset(deduped, limit, offset));
        }
    }

    // =========================================================
    // SELECT: AdvancedSelectQuery – handles joins, multi-agg
    //         GROUP BY, column aliases, ORDER BY alias, etc.
    // =========================================================

    public record AdvancedSelectQuery(
            String baseTable,
            String baseAlias,
            List<JoinSpec> joins,
            boolean isStar,
            List<String> regularCols,
            Map<String, String> colAliases,
            List<AggSpec> aggregates,
            List<String> groupByCols,
            WhereCondition where,
            WhereCondition having,
            boolean isDistinct,
            List<OrderByItem> orderBy,
            int limit,
            int offset
    ) implements QueryInterface {

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {

            // 1. Load base table rows
            List<Row> rows;
            if (joins.isEmpty()) {
                // No joins: unqualified row keys are fine
                rows = engine.select(baseTable, null, true, null, false, null, true);
            } else {
                // With joins: qualify all keys with "alias." (or tableName if no alias)
                rows = loadQualified(engine, baseTable, baseAlias);
            }

            // 2. Apply joins (inner / left)
            // Use alias as key prefix so the same table joined twice (different aliases) stays distinct.
            for (JoinSpec js : joins) {
                List<Row> rightRows = engine.select(js.tableName(), null, true, null, false, null, true);
                List<Row> newRows = new ArrayList<>();
                String jtPrefix = (js.alias() != null ? js.alias() : js.tableName()) + ".";

                for (Row leftRow : rows) {
                    String leftVal = rowGet(leftRow, js.leftKey());
                    boolean matched = false;

                    for (Row rightRow : rightRows) {
                        String rightVal = rightRow.get(js.rightKey());
                        if (leftVal != null && leftVal.equals(rightVal)) {
                            Map<String, String> combined = new LinkedHashMap<>(leftRow.getValuesMap());
                            rightRow.getValuesMap().forEach((k, v) -> combined.put(jtPrefix + k, v));
                            newRows.add(new Row(combined));
                            matched = true;
                        }
                    }
                    if (!matched && js.isLeftJoin()) {
                        newRows.add(leftRow);
                    }
                }
                rows = newRows;
            }

            // 3. Apply WHERE
            if (where != null) {
                rows = engine.applyWhereToJoinedRows(where, rows);
            }

            // 4. Compute result rows
            List<Row> result;
            if (!aggregates.isEmpty() && groupByCols.isEmpty()) {
                // Scalar aggregates (e.g. COUNT(*) AS total)
                result = computeScalarAggregates(rows);
            } else if (!groupByCols.isEmpty()) {
                // GROUP BY
                result = computeGroupBy(rows);
                if (having != null) {
                    result = result.stream()
                            .filter(r -> evaluateSimpleHaving(having, r))
                            .collect(Collectors.toList());
                }
            } else {
                // Plain projection
                result = project(rows);
            }

            // 5. Sort – ORDER BY may use qualified names or aliases; resolve lazily
            if (!orderBy.isEmpty() && !result.isEmpty()) {
                Row sample = result.get(0);
                Comparator<Row> comp = null;
                for (OrderByItem item : orderBy) {
                    String key = resolveOrderKey(item.col(), sample);
                    Comparator<Row> c = advSortComparator(key, item.asc());
                    comp = comp == null ? c : comp.thenComparing(c);
                }
                if (comp != null) {
                    List<Row> sorted = new ArrayList<>(result);
                    sorted.sort(comp);
                    result = sorted;
                }
            }

            // 6. DISTINCT + LIMIT
            if (isDistinct) result = new ArrayList<>(new LinkedHashSet<>(result));
            result = applyLimitOffset(result, limit, offset);

            return new ExecutionResult(true, "Query completed.", result);
        }

        // Load all rows from a table, pre-qualifying keys as "alias.column" (or tableName.column)
        private List<Row> loadQualified(DatabaseEngine engine, String tableName, String alias) throws Exception {
            List<Row> raw = engine.select(tableName, null, true, null, false, null, true);
            String prefix = (alias != null ? alias : tableName) + ".";
            List<Row> qualified = new ArrayList<>(raw.size());
            for (Row r : raw) {
                Map<String, String> q = new LinkedHashMap<>();
                r.getValuesMap().forEach((k, v) -> q.put(prefix + k, v));
                qualified.add(new Row(q));
            }
            return qualified;
        }

        // Lookup value by key; falls back to unqualified if qualified not found
        private String rowGet(Row row, String key) {
            String v = row.get(key);
            if (v != null) return v;
            int dot = key.lastIndexOf('.');
            if (dot >= 0) return row.get(key.substring(dot + 1));
            // Try suffix match for "anyTable.key"
            String suffix = "." + key;
            return row.getValuesMap().entrySet().stream()
                    .filter(e -> e.getKey().endsWith(suffix))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
        }

        // Compute GROUP BY: groups rows, computes aggregates, returns one row per group
        private List<Row> computeGroupBy(List<Row> rows) {
            Map<String, List<Row>> groups = new LinkedHashMap<>();

            for (Row row : rows) {
                String gkey = groupByCols.stream()
                        .map(c -> {
                            String v = rowGet(row, c);
                            return v != null ? v : " null ";
                        })
                        .collect(Collectors.joining(""));
                groups.computeIfAbsent(gkey, k -> new ArrayList<>()).add(row);
            }

            List<Row> result = new ArrayList<>(groups.size());
            for (List<Row> grp : groups.values()) {
                Map<String, String> out = new LinkedHashMap<>();

                // GROUP BY column values → use alias if provided, else unqualified key
                for (String col : groupByCols) {
                    String alias = colAliases.get(col);
                    String key = alias != null ? alias : unqualify(col);
                    out.put(key, rowGet(grp.get(0), col));
                }

                // Aggregate results
                for (AggSpec agg : aggregates) {
                    out.put(agg.alias(), computeAgg(agg.func(), agg.col(), grp));
                }
                result.add(new Row(out));
            }
            return result;
        }

        // Scalar aggregates: no GROUP BY, compute over all rows
        private List<Row> computeScalarAggregates(List<Row> rows) {
            Map<String, String> out = new LinkedHashMap<>();
            for (AggSpec agg : aggregates) {
                out.put(agg.alias(), computeAgg(agg.func(), agg.col(), rows));
            }
            return List.of(new Row(out));
        }

        // Project regular columns (no GROUP BY)
        private List<Row> project(List<Row> rows) {
            if (isStar && regularCols.isEmpty()) {
                // SELECT * – strip all table prefixes
                List<Row> out = new ArrayList<>(rows.size());
                for (Row r : rows) {
                    Map<String, String> m = new LinkedHashMap<>();
                    r.getValuesMap().forEach((k, v) -> m.put(unqualify(k), v));
                    out.add(new Row(m));
                }
                return out;
            }
            List<Row> out = new ArrayList<>(rows.size());
            for (Row r : rows) {
                Map<String, String> m = new LinkedHashMap<>();
                for (String col : regularCols) {
                    String val   = rowGet(r, col);
                    String alias = colAliases.get(col);
                    String key   = alias != null ? alias : unqualify(col);
                    m.put(key, val);
                }
                out.add(new Row(m));
            }
            return out;
        }

        // Compute one aggregate function over a list of rows
        private String computeAgg(String func, String col, List<Row> rows) {
            if ("COUNT".equals(func)) {
                if (col == null || "*".equals(col)) return String.valueOf(rows.size());
                long nn = rows.stream().filter(r -> rowGet(r, col) != null).count();
                return String.valueOf(nn);
            }
            if (col == null) throw new IllegalArgumentException(func + " requires a column");

            List<Double> nums = rows.stream()
                    .map(r -> rowGet(r, col))
                    .filter(Objects::nonNull)
                    .filter(v -> { try { Double.parseDouble(v); return true; } catch (NumberFormatException e) { return false; } })
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());

            if (nums.isEmpty()) {
                // Fall back to lexicographic MIN/MAX for non-numeric columns (e.g. dates)
                List<String> strs = rows.stream()
                        .map(r -> rowGet(r, col))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (strs.isEmpty()) return null;
                if ("MIN".equals(func)) return strs.stream().min(Comparator.naturalOrder()).orElse(null);
                if ("MAX".equals(func)) return strs.stream().max(Comparator.naturalOrder()).orElse(null);
                return null;
            }

            double res = switch (func) {
                case "SUM" -> nums.stream().mapToDouble(Double::doubleValue).sum();
                case "AVG" -> nums.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                case "MIN" -> nums.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                case "MAX" -> nums.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                default    -> throw new IllegalArgumentException("Unknown agg: " + func);
            };
            return fmtNum(res);
        }

        private String fmtNum(double v) {
            return (v == Math.floor(v) && !Double.isInfinite(v))
                    ? String.valueOf((long) v) : String.valueOf(v);
        }

        // Strip "tableName." prefix
        private String unqualify(String col) {
            int dot = col.lastIndexOf('.');
            return dot >= 0 ? col.substring(dot + 1) : col;
        }

        // Resolve ORDER BY key: direct → alias → unqualified
        private String resolveOrderKey(String col, Row sample) {
            if (sample.get(col) != null) return col;
            // Try the alias that was declared for this column
            String aliased = colAliases.get(col);
            if (aliased != null && sample.get(aliased) != null) return aliased;
            // Try unqualified (strip table/alias prefix)
            int dot = col.lastIndexOf('.');
            if (dot >= 0) {
                String unq = col.substring(dot + 1);
                if (sample.get(unq) != null) return unq;
            }
            return col;
        }

        private static Comparator<Row> advSortComparator(String col, boolean asc) {
            return (a, b) -> {
                String va = a.get(col);
                String vb = b.get(col);
                if (va == null && vb == null) return 0;
                if (va == null) return 1;
                if (vb == null) return -1;
                int cmp;
                try { cmp = Double.compare(Double.parseDouble(va), Double.parseDouble(vb)); }
                catch (NumberFormatException e) { cmp = va.compareTo(vb); }
                return asc ? cmp : -cmp;
            };
        }

        private boolean evaluateSimpleHaving(WhereCondition cond, Row row) {
            if (cond instanceof WhereCondition.Simple s) {
                String val = row.get(s.column());
                if (val == null) return false;
                try {
                    double dv = Double.parseDouble(val);
                    double dref = Double.parseDouble(s.value());
                    return switch (s.op()) {
                        case ">"  -> dv >  dref;
                        case ">=" -> dv >= dref;
                        case "<"  -> dv <  dref;
                        case "<=" -> dv <= dref;
                        case "="  -> dv == dref;
                        case "!=" -> dv != dref;
                        default   -> false;
                    };
                } catch (NumberFormatException e) {
                    int cmp = val.compareTo(s.value());
                    return switch (s.op()) {
                        case "="  -> cmp == 0;
                        case "!=" -> cmp != 0;
                        case ">"  -> cmp > 0;
                        case "<"  -> cmp < 0;
                        case ">=" -> cmp >= 0;
                        case "<=" -> cmp <= 0;
                        default   -> false;
                    };
                }
            }
            return true;
        }
    }

    // =========================================================
    // Scalar aggregate queries (legacy, kept for simple cases)
    // =========================================================

    public record CountQuery(String tableName, String columnName, WhereCondition where, String alias) implements QueryInterface {
        public CountQuery(String tableName, String columnName, WhereCondition where) {
            this(tableName, columnName, where, null);
        }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int count = engine.count(tableName, columnName, where);
            String key = alias != null ? alias : "COUNT";
            return new ExecutionResult(true, "Count: " + count,
                    List.of(new Row(Map.of(key, String.valueOf(count)))));
        }
    }

    public record SumQuery(String tableName, String columnName, WhereCondition where, String alias) implements QueryInterface {
        public SumQuery(String tableName, String columnName, WhereCondition where) {
            this(tableName, columnName, where, null);
        }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            double sum = engine.sum(tableName, columnName, where);
            String key = alias != null ? alias : "SUM";
            return new ExecutionResult(true, "Sum: " + sum,
                    List.of(new Row(Map.of(key, String.valueOf(sum)))));
        }
    }

    public record AvgQuery(String tableName, String columnName, WhereCondition where, String alias) implements QueryInterface {
        public AvgQuery(String tableName, String columnName, WhereCondition where) {
            this(tableName, columnName, where, null);
        }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            double avg = engine.avg(tableName, columnName, where);
            String key = alias != null ? alias : "AVG";
            return new ExecutionResult(true, "Avg: " + avg,
                    List.of(new Row(Map.of(key, String.valueOf(avg)))));
        }
    }

    public record MinQuery(String tableName, String columnName, WhereCondition where, String alias) implements QueryInterface {
        public MinQuery(String tableName, String columnName, WhereCondition where) {
            this(tableName, columnName, where, null);
        }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            String min = engine.min(tableName, columnName, where);
            String key = alias != null ? alias : "MIN";
            return new ExecutionResult(true, "Min: " + min,
                    List.of(new Row(Map.of(key, min != null ? min : "NULL"))));
        }
    }

    public record MaxQuery(String tableName, String columnName, WhereCondition where, String alias) implements QueryInterface {
        public MaxQuery(String tableName, String columnName, WhereCondition where) {
            this(tableName, columnName, where, null);
        }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            String max = engine.max(tableName, columnName, where);
            String key = alias != null ? alias : "MAX";
            return new ExecutionResult(true, "Max: " + max,
                    List.of(new Row(Map.of(key, max != null ? max : "NULL"))));
        }
    }

    // =========================================================
    // DML queries
    // =========================================================

    public record DeleteTableQuery(String tableName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int del = engine.delete(tableName, where);
            return new ExecutionResult(true, del + " row(s) deleted from '" + tableName + "'.");
        }
    }

    public record UpdateTableQuery(String tableName, Map<String, String> setValues,
                                   WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int upd = engine.update(tableName, setValues, where);
            return new ExecutionResult(true, upd + " row(s) updated in '" + tableName + "'.");
        }
    }

    // =========================================================
    // Transaction queries
    // =========================================================

    public record BeginTransactionQuery() implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) {
            engine.beginTransaction();
            return new ExecutionResult(true, "Transaction started.");
        }
    }

    public record CommitQuery() implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.commit();
            return new ExecutionResult(true, "Transaction committed.");
        }
    }

    public record RollbackQuery() implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) {
            engine.rollback();
            return new ExecutionResult(true, "Transaction rolled back.");
        }
    }

    // =========================================================
    // ALTER TABLE queries
    // =========================================================

    public record AlterTableAddColumnQuery(String tableName, ColumnMetadata column) implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.alterTableAddColumn(tableName, column);
            return new ExecutionResult(true, "Column added.");
        }
    }

    public record AlterTableDropColumnQuery(String tableName, String columnName) implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.alterTableDropColumn(tableName, columnName);
            return new ExecutionResult(true, "Column dropped.");
        }
    }

    public record AlterTableRenameColumnQuery(String tableName, String columnName, String newName) implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.alterTableRenameColumn(tableName, columnName, newName);
            return new ExecutionResult(true, "Column renamed.");
        }
    }

    public record AlterTableRenameTableQuery(String tableName, String newName) implements QueryInterface {
        @Override public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.alterTableRenameTable(tableName, newName);
            return new ExecutionResult(true, "Table renamed.");
        }
    }

    // =========================================================
    // Shared utilities
    // =========================================================

    static List<Row> applyLimitOffset(List<Row> rows, int limit, int offset) {
        if (limit < 0 && offset <= 0) return rows;
        int from = Math.min(Math.max(offset, 0), rows.size());
        int to   = limit < 0 ? rows.size() : Math.min(from + limit, rows.size());
        return new ArrayList<>(rows.subList(from, to));
    }

    static List<Row> multiSort(List<Row> rows, List<OrderByItem> orderBy) {
        if (orderBy == null || orderBy.isEmpty()) return rows;
        Comparator<Row> comp = null;
        for (OrderByItem item : orderBy) {
            Comparator<Row> c = sortComparator(item.col(), item.asc());
            comp = comp == null ? c : comp.thenComparing(c);
        }
        List<Row> sorted = new ArrayList<>(rows);
        if (comp != null) sorted.sort(comp);
        return sorted;
    }

    private static Comparator<Row> sortComparator(String col, boolean asc) {
        return (a, b) -> {
            String va = a.get(col);
            String vb = b.get(col);
            if (va == null && vb == null) return 0;
            if (va == null) return 1;
            if (vb == null) return -1;
            int cmp;
            try { cmp = Double.compare(Double.parseDouble(va), Double.parseDouble(vb)); }
            catch (NumberFormatException e) { cmp = va.compareTo(vb); }
            return asc ? cmp : -cmp;
        };
    }

    static List<Row> applyAliases(List<Row> rows, Map<String, String> aliases) {
        if (aliases == null || aliases.isEmpty()) return rows;
        List<Row> result = new ArrayList<>(rows.size());
        for (Row row : rows) {
            Map<String, String> newVals = new LinkedHashMap<>();
            row.getValuesMap().forEach((k, v) -> {
                String alias = aliases.get(k);
                if (alias == null) {
                    int dot = k.lastIndexOf('.');
                    if (dot >= 0) alias = aliases.get(k.substring(dot + 1));
                }
                newVals.put(alias != null ? alias : k, v);
            });
            result.add(new Row(newVals));
        }
        return result;
    }
}
