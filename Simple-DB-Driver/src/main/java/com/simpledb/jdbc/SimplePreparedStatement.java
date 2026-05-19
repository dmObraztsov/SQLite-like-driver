package com.simpledb.jdbc;

import SqlParser.QueriesStruct.ExecutionResult;
import Yadro.DataStruct.Row;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * PreparedStatement поверх нашего движка.
 *
 * Стратегия: храним SQL-шаблон с ?, накапливаем параметры, при execute()
 * заменяем ? на литералы и передаём в SimpleStatement.
 */
public class SimplePreparedStatement implements PreparedStatement {

    private final SimpleConnection conn;
    private final String sqlTemplate;
    private final List<Object> params = new ArrayList<>();

    private ResultSet currentResultSet;
    private int updateCount = -1;
    private boolean closed = false;
    private long lastGeneratedKey = -1;

    SimplePreparedStatement(SimpleConnection conn, String sql) {
        this.conn = conn;
        this.sqlTemplate = sql;
        int count = countPlaceholders(sql);
        for (int i = 0; i < count; i++) params.add(null);
    }

    // ---- параметры ----

    @Override public void setNull(int i, int sqlType) { set(i, null); }
    @Override public void setNull(int i, int sqlType, String typeName) { set(i, null); }

    @Override public void setBoolean(int i, boolean x) { set(i, x ? "1" : "0"); }
    @Override public void setByte(int i, byte x)       { set(i, (long) x); }
    @Override public void setShort(int i, short x)     { set(i, (long) x); }
    @Override public void setInt(int i, int x)         { set(i, (long) x); }
    @Override public void setLong(int i, long x)       { set(i, x); }
    @Override public void setFloat(int i, float x)     { set(i, (double) x); }
    @Override public void setDouble(int i, double x)   { set(i, x); }
    @Override public void setBigDecimal(int i, BigDecimal x) { set(i, x == null ? null : x.toPlainString()); }
    @Override public void setString(int i, String x)   { set(i, x); }
    @Override public void setNString(int i, String x)  { set(i, x); }
    @Override public void setBytes(int i, byte[] x)    { set(i, x == null ? null : new String(x)); }

    @Override public void setDate(int i, java.sql.Date x)            { set(i, x == null ? null : x.toString()); }
    @Override public void setDate(int i, java.sql.Date x, Calendar c){ setDate(i, x); }
    @Override public void setTime(int i, Time x)                     { set(i, x == null ? null : x.toString()); }
    @Override public void setTime(int i, Time x, Calendar c)         { setTime(i, x); }
    @Override public void setTimestamp(int i, Timestamp x)           { set(i, x == null ? null : x.toString()); }
    @Override public void setTimestamp(int i, Timestamp x, Calendar c){ setTimestamp(i, x); }

    @Override
    public void setObject(int i, Object x) {
        if (x == null)                         set(i, null);
        else if (x instanceof String s)        set(i, s);
        else if (x instanceof Integer n)       set(i, (long) n);
        else if (x instanceof Long n)          set(i, n);
        else if (x instanceof Double d)        set(i, d);
        else if (x instanceof Float f)         set(i, (double) f);
        else if (x instanceof Boolean b)       set(i, b ? "1" : "0");
        else if (x instanceof java.sql.Date d) set(i, d.toString());
        else if (x instanceof Timestamp ts)    set(i, ts.toString());
        else                                   set(i, x.toString());
    }

    @Override public void setObject(int i, Object x, int sqlType)           { setObject(i, x); }
    @Override public void setObject(int i, Object x, int sqlType, int scale){ setObject(i, x); }
    @Override public void setObject(int i, Object x, SQLType t)             { setObject(i, x); }
    @Override public void setObject(int i, Object x, SQLType t, int scale)  { setObject(i, x); }

    private void set(int parameterIndex, Object value) {
        int idx = parameterIndex - 1;
        while (params.size() <= idx) params.add(null);
        params.set(idx, value);
    }

    // ---- построение SQL ----

    private String buildSql() throws SQLException {
        StringBuilder sb = new StringBuilder();
        int paramIdx = 0;
        for (int i = 0; i < sqlTemplate.length(); i++) {
            char c = sqlTemplate.charAt(i);
            if (c == '?') {
                if (paramIdx >= params.size()) {
                    throw new SQLException("Параметр " + (paramIdx + 1) + " не установлен");
                }
                appendParam(sb, params.get(paramIdx++));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void appendParam(StringBuilder sb, Object val) {
        if (val == null) {
            sb.append("NULL");
        } else if (val instanceof Float f) {
            if (f == Math.floor(f) && !Float.isInfinite(f)) sb.append((long) (float) f);
            else sb.append(f);
        } else if (val instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) sb.append((long) (double) d);
            else sb.append(d);
        } else if (val instanceof Number n) {
            sb.append(n.longValue());
        } else {
            String s = val.toString();
            sb.append('"').append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
    }

    // ---- execute ----

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkClosed();
        String sql = buildSql();
        conn.ensureTransactionIfNeeded();
        SimpleStatement st = new SimpleStatement(conn);
        currentResultSet = st.executeQuery(sql);
        updateCount = -1;
        return currentResultSet;
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkClosed();
        String sql = buildSql();
        conn.ensureTransactionIfNeeded();
        SimpleStatement st = new SimpleStatement(conn);
        updateCount = st.executeUpdate(sql);
        lastGeneratedKey = conn.getEngine().getLastGeneratedKey();
        currentResultSet = null;
        return updateCount;
    }

    @Override
    public boolean execute() throws SQLException {
        checkClosed();
        String sql = buildSql();
        conn.ensureTransactionIfNeeded();
        SimpleStatement st = new SimpleStatement(conn);
        boolean hasRs = st.execute(sql);
        lastGeneratedKey = conn.getEngine().getLastGeneratedKey();
        if (hasRs) {
            currentResultSet = st.getResultSet();
            updateCount = -1;
        } else {
            currentResultSet = null;
            updateCount = st.getUpdateCount();
        }
        return hasRs;
    }

    @Override public long executeLargeUpdate() throws SQLException { return executeUpdate(); }

    @Override public void clearParameters() {
        for (int i = 0; i < params.size(); i++) params.set(i, null);
    }

    // ---- состояние ----

    @Override public ResultSet getResultSet()       { return currentResultSet; }
    @Override public int getUpdateCount()           { return updateCount; }
    @Override public boolean getMoreResults()       { updateCount = -1; return false; }
    @Override public boolean getMoreResults(int c)  { updateCount = -1; return false; }
    @Override public Connection getConnection()     { return conn; }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        if (lastGeneratedKey < 0) {
            return new SimpleResultSet(new ExecutionResult(true, "", List.of()), this);
        }
        Row row = new Row(Map.of("id", String.valueOf(lastGeneratedKey)));
        return new SimpleResultSet(new ExecutionResult(true, "", List.of(row)), this);
    }

    @Override
    public void close() throws SQLException {
        if (currentResultSet != null) {
            try { currentResultSet.close(); } catch (SQLException ignored) {}
        }
        closed = true;
    }

    @Override public boolean isClosed() { return closed; }

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("PreparedStatement закрыт");
    }

    // ---- Statement-методы (для сырого SQL) ----

    @Override public ResultSet executeQuery(String sql) throws SQLException {
        return new SimpleStatement(conn).executeQuery(sql);
    }

    @Override public int executeUpdate(String sql) throws SQLException {
        return new SimpleStatement(conn).executeUpdate(sql);
    }

    @Override public boolean execute(String sql) throws SQLException {
        return new SimpleStatement(conn).execute(sql);
    }

    @Override public int executeUpdate(String sql, int x)      throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, int[] x)    throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, String[] x) throws SQLException { return executeUpdate(sql); }
    @Override public boolean execute(String sql, int x)        throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, int[] x)      throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, String[] x)   throws SQLException { return execute(sql); }

    // ---- заглушки ----

    @Override public void addBatch()               throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void addBatch(String sql)     throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void clearBatch()             throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int[] executeBatch()          throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override public ResultSetMetaData getMetaData()             { return null; }
    @Override public ParameterMetaData getParameterMetaData()    throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override public int getMaxFieldSize()              { return 0; }
    @Override public void setMaxFieldSize(int max)      {}
    @Override public int getMaxRows()                   { return 0; }
    @Override public void setMaxRows(int max)           {}
    @Override public void setEscapeProcessing(boolean e){}
    @Override public int getQueryTimeout()              { return 0; }
    @Override public void setQueryTimeout(int sec)      {}
    @Override public void cancel()                      {}
    @Override public SQLWarning getWarnings()           { return null; }
    @Override public void clearWarnings()               {}
    @Override public void setCursorName(String name)    {}
    @Override public int getFetchDirection()            { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchDirection(int d)      {}
    @Override public int getFetchSize()                 { return 0; }
    @Override public void setFetchSize(int rows)        {}
    @Override public int getResultSetConcurrency()      { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType()             { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public int getResultSetHoldability()      { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public boolean isPoolable()               { return false; }
    @Override public void setPoolable(boolean p)        {}
    @Override public void closeOnCompletion()           {}
    @Override public boolean isCloseOnCompletion()      { return false; }

    @Override public void setAsciiStream(int i, InputStream x, int l)   throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setAsciiStream(int i, InputStream x, long l)  throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setAsciiStream(int i, InputStream x)          throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setUnicodeStream(int i, InputStream x, int l) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int i, InputStream x, int l)  throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int i, InputStream x, long l) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int i, InputStream x)         throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int i, Reader x, int l)    throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int i, Reader x, long l)   throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int i, Reader x)           throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNCharacterStream(int i, Reader x, long l)  throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNCharacterStream(int i, Reader x)          throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setRef(int i, Ref x)       throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int i, Blob x)     throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int i, InputStream x, long l) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int i, InputStream x)         throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int i, Clob x)     throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int i, Reader x, long l)      throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int i, Reader x)              throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int i, NClob x)   throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int i, Reader x, long l)     throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int i, Reader x)             throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setArray(int i, Array x)   throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setSQLXML(int i, SQLXML x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setRowId(int i, RowId x)   throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setURL(int i, URL x)       throws SQLException { throw new SQLFeatureNotSupportedException(); }

    private static int countPlaceholders(String sql) {
        int count = 0;
        boolean inStr = false;
        char strChar = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inStr) {
                if (c == '\\') { i++; continue; }
                if (c == strChar) inStr = false;
            } else if (c == '\'' || c == '"') {
                inStr = true; strChar = c;
            } else if (c == '?') {
                count++;
            }
        }
        return count;
    }

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
