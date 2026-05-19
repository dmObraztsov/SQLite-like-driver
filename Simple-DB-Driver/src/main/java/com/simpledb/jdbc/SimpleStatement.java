package com.simpledb.jdbc;

import SqlParser.Antlr.SQLProcessor;
import SqlParser.QueriesStruct.ExecutionResult;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.DatabaseEngine;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDBC Statement — выполнение SQL.
 */
public class SimpleStatement implements Statement {

    private static final Pattern AFFECTED_ROWS = Pattern.compile("^(\\d+) row");

    private final SimpleConnection conn;
    private final DatabaseEngine engine;
    private boolean closed = false;
    private ResultSet currentResultSet;
    private int updateCount = -1;
    private long lastGeneratedKey = -1;
    private int maxRows = 0;
    private int fetchSize = 0;
    private int queryTimeout = 0;

    SimpleStatement(SimpleConnection conn) {
        this.conn = conn;
        this.engine = conn.getEngine();
    }

    //execute

    @Override
    public boolean execute(String sql) throws SQLException {
        checkClosed();
        ExecutionResult result = run(sql);
        if (isSelectLike(sql)) {
            currentResultSet = new SimpleResultSet(applyMaxRows(result), this);
            updateCount = -1;
            return true;
        } else {
            currentResultSet = null;
            updateCount = parseAffectedRows(result.getMessage());
            return false;
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkClosed();
        if (!isSelectLike(sql)) {
            throw new SQLException("executeQuery ожидает SELECT-подобный запрос, получен: " + sql);
        }
        ExecutionResult result = run(sql);
        currentResultSet = new SimpleResultSet(applyMaxRows(result), this);
        updateCount = -1;
        return currentResultSet;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        checkClosed();
        if (isSelectLike(sql)) {
            throw new SQLException("executeUpdate не для SELECT: " + sql);
        }
        ExecutionResult result = run(sql);
        currentResultSet = null;
        updateCount = parseAffectedRows(result.getMessage());
        lastGeneratedKey = engine.getLastGeneratedKey();
        return updateCount;
    }

    private ExecutionResult applyMaxRows(ExecutionResult result) {
        if (maxRows > 0 && result.getRows().size() > maxRows) {
            result.setRows(new java.util.ArrayList<>(result.getRows().subList(0, maxRows)));
        }
        return result;
    }

    private ExecutionResult run(String sql) throws SQLException {
        conn.ensureTransactionIfNeeded();
        QueryInterface query = SQLProcessor.getQuery(sql);
        if (query == null) {
            throw new SQLSyntaxErrorException("Не удалось распарсить SQL: " + sql);
        }
        try {
            ExecutionResult result = query.execute(engine);
            if (!result.isSuccess()) {
                throw new SQLException("Запрос упал: " + result.getMessage());
            }
            return result;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Ошибка выполнения: " + e.getMessage(), e);
        }
    }

    /**
     * SELECT возвращает rows, агрегаты тоже возвращают rows.
     */
    private boolean isSelectLike(String sql) {
        String trimmed = sql.trim().toUpperCase();
        return trimmed.startsWith("SELECT");
    }

    /**
     * Движок возвращает сообщение вида "3 row(s) deleted from ..." или
     * "Row inserted into ...". Для INSERT всегда 1, для UPDATE/DELETE парсим число.
     */
    private int parseAffectedRows(String message) {
        if (message == null){
            return 0;
        }
        if (message.startsWith("Row inserted")) {
            return 1;
        }
        Matcher m = AFFECTED_ROWS.matcher(message);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    // state/результаты

    @Override public ResultSet getResultSet() { return currentResultSet; }
    @Override public int getUpdateCount() { return updateCount; }
    @Override public boolean getMoreResults() { updateCount = -1; return false; }
    @Override public boolean getMoreResults(int current) { updateCount = -1; return false; }
    @Override public Connection getConnection() { return conn; }

    @Override
    public void close() {
        if (currentResultSet != null) {
            try { currentResultSet.close(); } catch (SQLException ignored) {}
        }
        closed = true;
    }

    @Override public boolean isClosed() { return closed; }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException("Statement закрыт");
        }
    }


    @Override public int getMaxRows() { return maxRows; }
    @Override public void setMaxRows(int max) { this.maxRows = max; }
    @Override public int getMaxFieldSize() { return 0; }
    @Override public void setMaxFieldSize(int max) {}
    @Override public void setEscapeProcessing(boolean enable) {}
    @Override public int getQueryTimeout() { return queryTimeout; }
    @Override public void setQueryTimeout(int seconds) { this.queryTimeout = seconds; }
    @Override public void cancel() {}
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public void setCursorName(String name) {}
    @Override public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchDirection(int direction) {}
    @Override public int getFetchSize() { return fetchSize; }
    @Override public void setFetchSize(int rows) { this.fetchSize = rows; }
    @Override public int getResultSetConcurrency() { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType() { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public int getResultSetHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public boolean isPoolable() { return false; }
    @Override public void setPoolable(boolean poolable) {}
    @Override public void closeOnCompletion() {}
    @Override public boolean isCloseOnCompletion() { return false; }

    //batch нет у нас
    @Override public void addBatch(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void clearBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int[] executeBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        if (lastGeneratedKey < 0) {
            return new SimpleResultSet(
                    new SqlParser.QueriesStruct.ExecutionResult(true, "", java.util.List.of()), this);
        }
        Yadro.DataStruct.Row row = new Yadro.DataStruct.Row(
                java.util.Map.of("id", String.valueOf(lastGeneratedKey)));
        return new SimpleResultSet(
                new SqlParser.QueriesStruct.ExecutionResult(true, "", java.util.List.of(row)), this);
    }
    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { return executeUpdate(sql); }
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return execute(sql); }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
