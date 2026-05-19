package SqlParser.QueriesStruct;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;

import java.util.*;
import java.util.LinkedHashMap;

public class Queries {

    public record OrderByItem(String col, boolean asc) {}


    public record CreateDataBaseQuery(String databaseName, boolean ifNotExists) implements QueryInterface {
        public CreateDataBaseQuery(String databaseName) { this(databaseName, false); }
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            try {
                engine.createDatabase(databaseName);
            } catch (Exception e) {
                if (!ifNotExists) throw e;
            }
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
            try {
                engine.dropTable(tableName);
            } catch (Exception e) {
                if (!ifExists) throw e;
            }
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

    public record SelectDataQuery(List<String> selectCols, boolean isStar, String tableName,
                                  WhereCondition where, boolean isDistinct,
                                  List<OrderByItem> orderBy,
                                  Map<String, String> aliases,
                                  int limit, int offset) implements QueryInterface {
        // backward-compat: old String orderByCol, boolean orderByAsc style (used by tests)
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

        // new-style with List<OrderByItem> but no aliases (for parser before alias support)
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

    public record GroupBySelectQuery(String tableName, String groupByCol,
                                     String aggFunc, String aggCol,
                                     WhereCondition where, WhereCondition having,
                                     List<String> extraSelectCols,
                                     List<OrderByItem> orderBy,
                                     int limit, int offset) implements QueryInterface {
        public GroupBySelectQuery(String tableName, String groupByCol,
                                  String aggFunc, String aggCol,
                                  WhereCondition where, WhereCondition having,
                                  List<String> extraSelectCols,
                                  String orderByCol, boolean orderByAsc) {
            this(tableName, groupByCol, aggFunc, aggCol, where, having, extraSelectCols,
                 orderByCol != null ? List.of(new OrderByItem(orderByCol, orderByAsc)) : List.of(),
                 -1, 0);
        }

        public GroupBySelectQuery(String tableName, String groupByCol,
                                  String aggFunc, String aggCol,
                                  WhereCondition where, WhereCondition having,
                                  List<String> extraSelectCols,
                                  String orderByCol, boolean orderByAsc, int limit, int offset) {
            this(tableName, groupByCol, aggFunc, aggCol, where, having, extraSelectCols,
                 orderByCol != null ? List.of(new OrderByItem(orderByCol, orderByAsc)) : List.of(),
                 limit, offset);
        }

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> results = engine.groupBySelect(
                    tableName, groupByCol, aggFunc, aggCol,
                    where, having, extraSelectCols, null, true
            );
            return new ExecutionResult(true, "Group by completed.",
                    applyLimitOffset(multiSort(results, orderBy), limit, offset));
        }
    }

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

        // backward-compat: isLeftJoin + limit/offset but no orderBy/aliases
        public JoinTableQuery(String table1Name, List<String> columns1,
                              String table2Name, List<String> columns2,
                              String leftJoinCol, String rightJoinCol,
                              boolean isDistinct, WhereCondition where,
                              boolean isLeftJoin, int limit, int offset) {
            this(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol,
                 isDistinct, where, isLeftJoin, List.of(), Map.of(), limit, offset);
        }

        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            List<Row> rawJoinedRows = engine.join(table1Name, columns1, table2Name, columns2, leftJoinCol, rightJoinCol, isLeftJoin);

            List<Row> filtered = (where == null) ? rawJoinedRows : engine.applyWhereToJoinedRows(where, rawJoinedRows);

            List<Row> projectedRows = new ArrayList<>();
            for (Row row : filtered) {
                if (columns1 == null && columns2 == null) {
                    projectedRows.add(row);
                    continue;
                }

                Map<String, String> filteredValues = new HashMap<>();
                if (columns1 != null) {
                    for (String col : columns1) {
                        String fullKey = table1Name + "." + col;
                        String value = row.get(fullKey);
                        if (value != null) {
                            filteredValues.put(fullKey, value);
                            if (!col.equalsIgnoreCase("id")) {
                                filteredValues.putIfAbsent(col, value);
                            }
                        }
                    }
                }
                if (columns2 != null) {
                    for (String col : columns2) {
                        String fullKey = table2Name + "." + col;
                        String value = row.get(fullKey);
                        if (value != null) {
                            filteredValues.put(fullKey, value);
                            if (!col.equalsIgnoreCase("id")) {
                                filteredValues.putIfAbsent(col, value);
                            }
                        }
                    }
                }
                projectedRows.add(new Row(filteredValues));
            }

            List<Row> sorted = multiSort(projectedRows, orderBy);
            List<Row> aliased = applyAliases(sorted, aliases);
            List<Row> finalResult = aliased;
            if (this.isDistinct) {
                finalResult = new ArrayList<>(new LinkedHashSet<>(aliased));
            }
            return new ExecutionResult(true, "Join completed", applyLimitOffset(finalResult, limit, offset));
        }
    }

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
            comp = (comp == null) ? c : comp.thenComparing(c);
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
            try {
                cmp = Double.compare(Double.parseDouble(va), Double.parseDouble(vb));
            } catch (NumberFormatException e) {
                cmp = va.compareTo(vb);
            }
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

    public record BeginTransactionQuery() implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) {
            engine.beginTransaction();
            return new ExecutionResult(true, "Transaction started.");
        }
    }

    public record CommitQuery() implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception {
            engine.commit();
            return new ExecutionResult(true, "Transaction committed.");
        }
    }

    public record RollbackQuery() implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) {
            engine.rollback();
            return new ExecutionResult(true, "Transaction rolled back.");
        }
    }

    public record AlterTableAddColumnQuery(String tableName, ColumnMetadata column) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableAddColumn(tableName, column);
            return new ExecutionResult(true, "Column '" + column.getName() + "' added to table '" + tableName + "'.");
        }
    }

    public record AlterTableDropColumnQuery(String tableName, String columnName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableDropColumn(tableName, columnName);
            return new ExecutionResult(true, "Column '" + columnName + "' dropped from table '" + tableName + "'.");
        }
    }

    public record AlterTableRenameColumnQuery(String tableName, String columnName, String newName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableRenameColumn(tableName, columnName, newName);
            return new ExecutionResult(true, "Column '" + columnName + "' renamed to '" + newName + "'.");
        }
    }

    public record AlterTableRenameTableQuery(String tableName, String newName) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            engine.alterTableRenameTable(tableName, newName);
            return new ExecutionResult(true, "Table '" + tableName + "' renamed to '" + newName + "'.");
        }
    }

    public record DeleteTableQuery(String tableName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int deletedRows = engine.delete(tableName, where);
            String message = (deletedRows > 0)
                    ? deletedRows + " row(s) deleted from '" + tableName + "'."
                    : "No matching rows found in '" + tableName + "'.";
            return new ExecutionResult(true, message);
        }
    }

    public record UpdateTableQuery(String tableName, Map<String, String> setValues,
                                   WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int updatedRows = engine.update(tableName, setValues, where);
            String message = (updatedRows > 0)
                    ? updatedRows + " row(s) updated in '" + tableName + "'."
                    : "No matching rows found in '" + tableName + "'.";
            return new ExecutionResult(true, message);
        }
    }

    public record CountQuery(String tableName, String columnName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            int count = engine.count(tableName, columnName, where);
            return new ExecutionResult(true, "Count result: " + count,
                    List.of(new Row(Map.of("COUNT", String.valueOf(count)))));
        }
    }

    public record SumQuery(String tableName, String columnName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            double sum = engine.sum(tableName, columnName, where);
            return new ExecutionResult(true, "Sum result: " + sum,
                    List.of(new Row(Map.of("SUM", String.valueOf(sum)))));
        }
    }

    public record AvgQuery(String tableName, String columnName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            double avg = engine.avg(tableName, columnName, where);
            return new ExecutionResult(true, "Avg result: " + avg,
                    List.of(new Row(Map.of("AVG", String.valueOf(avg)))));
        }
    }

    public record MinQuery(String tableName, String columnName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            String min = engine.min(tableName, columnName, where);
            return new ExecutionResult(true, "Min result: " + min,
                    List.of(new Row(Map.of("MIN", min != null ? min : "NULL"))));
        }
    }

    public record MaxQuery(String tableName, String columnName, WhereCondition where) implements QueryInterface {
        @Override
        public ExecutionResult execute(DatabaseEngine engine) throws Exception, FileStorageException {
            String max = engine.max(tableName, columnName, where);
            return new ExecutionResult(true, "Max result: " + max,
                    List.of(new Row(Map.of("MAX", max != null ? max : "NULL"))));
        }
    }
}
