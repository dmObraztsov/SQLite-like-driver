package com.simpledb.jdbc;

import SqlParser.QueriesStruct.ExecutionResult;
import Yadro.DataStruct.Row;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * JDBC ResultSet — курсор по результату SELECT.
 *
 * Особенности нашего движка:
 *   <p>- Row.values это HashMap, порядок ключей не гарантирован.
 *     Фиксируем порядок один раз — алфавитно по первой Row.
 *   <p>- все значения хранятся как String, типизация — парсинг в getInt/getDouble/...
 *   <p>- значение "NULL" (буквально строка) трактуем как SQL NULL.
 */
public class SimpleResultSet implements ResultSet {

    private final List<Row> rows;
    private final List<String> columnNames;       // фиксированный порядок
    private final Map<String, Integer> nameToIdx; // 1-based индексы
    private final SimpleResultSetMetaData metaData;
    private final Statement parent;

    private int cursor = -1; // курсор перед первой строкой
    private boolean closed = false;
    private boolean wasNull = false;

    SimpleResultSet(ExecutionResult result, Statement parent) {
        this.parent = parent;
        this.rows = (result.getRows() == null) ? List.of() : new ArrayList<>(result.getRows());

        // Фиксируем порядок колонок: берём ключи первой Row, сортируем алфавитно
        // (детерминированно между запусками, в отличие от HashMap iteration order).
        if (this.rows.isEmpty()) {
            this.columnNames = List.of();
        } else {
            List<String> sorted = new ArrayList<>(this.rows.get(0).getValuesMap().keySet());
            Collections.sort(sorted);
            this.columnNames = Collections.unmodifiableList(sorted);
        }

        this.nameToIdx = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            // case-insensitive lookup
            nameToIdx.put(columnNames.get(i).toLowerCase(), i + 1);
        }

        this.metaData = new SimpleResultSetMetaData(columnNames);
    }

    // навигация

    @Override
    public boolean next() throws SQLException {
        checkClosed();
        cursor++;
        return cursor < rows.size();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override public boolean isClosed() { return closed; }
    @Override public boolean isBeforeFirst() { return cursor < 0; }
    @Override public boolean isAfterLast() { return cursor >= rows.size(); }
    @Override public boolean isFirst() { return cursor == 0; }
    @Override public boolean isLast() { return cursor == rows.size() - 1; }

    //чтение значений

    private String rawValue(int columnIndex) throws SQLException {
        checkClosed();
        if (cursor < 0 || cursor >= rows.size()) {
            throw new SQLException("Курсор вне диапазона строк");
        }
        if (columnIndex < 1 || columnIndex > columnNames.size()) {
            throw new SQLException("Невалидный индекс колонки: " + columnIndex);
        }
        String col = columnNames.get(columnIndex - 1);
        String value = rows.get(cursor).get(col);
        wasNull = (value == null || "NULL".equals(value));
        return wasNull ? null : value;
    }

    private String rawValue(String columnLabel) throws SQLException {
        checkClosed();
        Integer idx = nameToIdx.get(columnLabel.toLowerCase());
        if (idx == null) {
            throw new SQLException("Нет колонки: " + columnLabel);
        }
        return rawValue(idx);
    }

    @Override public boolean wasNull() { return wasNull; }

    @Override public String getString(int columnIndex) throws SQLException { return rawValue(columnIndex); }
    @Override public String getString(String columnLabel) throws SQLException { return rawValue(columnLabel); }
    @Override public String getNString(int columnIndex) throws SQLException { return getString(columnIndex); }
    @Override public String getNString(String columnLabel) throws SQLException { return getString(columnLabel); }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) return 0;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { throw new SQLDataException("Не число: " + v); }
    }
    @Override public int getInt(String columnLabel) throws SQLException { return getInt(requireIdx(columnLabel)); }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) return 0L;
        try { return Long.parseLong(v.trim()); }
        catch (NumberFormatException e) { throw new SQLDataException("Не long: " + v); }
    }
    @Override public long getLong(String columnLabel) throws SQLException { return getLong(requireIdx(columnLabel)); }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) return 0.0d;
        try { return Double.parseDouble(v.trim()); }
        catch (NumberFormatException e) { throw new SQLDataException("Не double: " + v); }
    }
    @Override public double getDouble(String columnLabel) throws SQLException { return getDouble(requireIdx(columnLabel)); }

    @Override public float getFloat(int columnIndex) throws SQLException { return (float) getDouble(columnIndex); }
    @Override public float getFloat(String columnLabel) throws SQLException { return (float) getDouble(columnLabel); }

    @Override public short getShort(int columnIndex) throws SQLException { return (short) getInt(columnIndex); }
    @Override public short getShort(String columnLabel) throws SQLException { return (short) getInt(columnLabel); }
    @Override public byte getByte(int columnIndex) throws SQLException { return (byte) getInt(columnIndex); }
    @Override public byte getByte(String columnLabel) throws SQLException { return (byte) getInt(columnLabel); }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) {
            return false;
        }
        return v.equalsIgnoreCase("true") || v.equals("1");
    }
    @Override public boolean getBoolean(String columnLabel) throws SQLException { return getBoolean(requireIdx(columnLabel)); }

    @Override
    public Object getObject(int columnIndex) throws SQLException { return rawValue(columnIndex); }
    @Override public Object getObject(String columnLabel) throws SQLException { return rawValue(columnLabel); }
    @Override public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        Object v = getObject(columnIndex);
        if (v == null) {
            return null;
        }
        if (type.isInstance(v)) {
            return type.cast(v);
        }
        throw new SQLException("Не могу привести к " + type);
    }
    @Override public <T> T getObject(String columnLabel, Class<T> type) throws SQLException { return getObject(requireIdx(columnLabel), type); }
    @Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException { return getObject(columnIndex); }
    @Override public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException { return getObject(columnLabel); }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        return v == null ? null : new BigDecimal(v.trim());
    }
    @Override public BigDecimal getBigDecimal(String columnLabel) throws SQLException { return getBigDecimal(requireIdx(columnLabel)); }
    @Override @Deprecated public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return getBigDecimal(columnIndex); }
    @Override @Deprecated public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException { return getBigDecimal(columnLabel); }

    private int requireIdx(String columnLabel) throws SQLException {
        Integer idx = nameToIdx.get(columnLabel.toLowerCase());
        if (idx == null) {
            throw new SQLException("Нет колонки: " + columnLabel);
        }
        return idx;
    }

    // метаданные

    @Override public ResultSetMetaData getMetaData() { return metaData; }
    @Override public Statement getStatement() { return parent; }
    @Override public int findColumn(String columnLabel) throws SQLException { return requireIdx(columnLabel); }
    @Override public String getCursorName() { return null; }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() { /* no-op */ }
    @Override public int getRow() { return cursor + 1; }
    @Override public int getType() { return TYPE_FORWARD_ONLY; }
    @Override public int getConcurrency() { return CONCUR_READ_ONLY; }
    @Override public int getHoldability() { return CLOSE_CURSORS_AT_COMMIT; }
    @Override public int getFetchDirection() { return FETCH_FORWARD; }
    @Override public void setFetchDirection(int direction) { /* no-op */ }
    @Override public int getFetchSize() { return 0; }
    @Override public void setFetchSize(int rows) { /* no-op */ }

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("ResultSet закрыт");
    }

    //заглушки

    @Override public void beforeFirst() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void afterLast() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean first() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean last() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean absolute(int row) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean relative(int rows) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean previous() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean rowUpdated() { return false; }
    @Override public boolean rowInserted() { return false; }
    @Override public boolean rowDeleted() { return false; }

    @Override public byte[] getBytes(int columnIndex) throws SQLException { String v = rawValue(columnIndex); return v == null ? null : v.getBytes(); }
    @Override public byte[] getBytes(String columnLabel) throws SQLException { return getBytes(requireIdx(columnLabel)); }

    @Override
    public java.sql.Date getDate(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) return null;
        try { return java.sql.Date.valueOf(v.trim()); }
        catch (IllegalArgumentException e) { throw new SQLDataException("Cannot parse date: " + v); }
    }
    @Override public java.sql.Date getDate(String columnLabel) throws SQLException { return getDate(requireIdx(columnLabel)); }
    @Override public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException { return getDate(columnIndex); }
    @Override public java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException { return getDate(columnLabel); }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) return null;
        try { return Time.valueOf(v.trim()); }
        catch (IllegalArgumentException e) { throw new SQLDataException("Cannot parse time: " + v); }
    }
    @Override public Time getTime(String columnLabel) throws SQLException { return getTime(requireIdx(columnLabel)); }
    @Override public Time getTime(int columnIndex, Calendar cal) throws SQLException { return getTime(columnIndex); }
    @Override public Time getTime(String columnLabel, Calendar cal) throws SQLException { return getTime(columnLabel); }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String v = rawValue(columnIndex);
        if (v == null) return null;
        try { return Timestamp.valueOf(v.trim()); }
        catch (IllegalArgumentException e) {
            try { return Timestamp.valueOf(v.trim() + " 00:00:00"); }
            catch (IllegalArgumentException e2) { throw new SQLDataException("Cannot parse timestamp: " + v); }
        }
    }
    @Override public Timestamp getTimestamp(String columnLabel) throws SQLException { return getTimestamp(requireIdx(columnLabel)); }
    @Override public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException { return getTimestamp(columnIndex); }
    @Override public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException { return getTimestamp(columnLabel); }

    @Override public InputStream getAsciiStream(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public InputStream getAsciiStream(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override @Deprecated public InputStream getUnicodeStream(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override @Deprecated public InputStream getUnicodeStream(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public InputStream getBinaryStream(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public InputStream getBinaryStream(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Reader getCharacterStream(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Reader getCharacterStream(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Reader getNCharacterStream(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Reader getNCharacterStream(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Blob getBlob(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Blob getBlob(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Clob getClob(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Clob getClob(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public NClob getNClob(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public NClob getNClob(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Array getArray(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Array getArray(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Ref getRef(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Ref getRef(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public URL getURL(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public URL getURL(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public RowId getRowId(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public RowId getRowId(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public SQLXML getSQLXML(int columnIndex) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public SQLXML getSQLXML(String columnLabel) throws SQLException { throw new SQLFeatureNotSupportedException(); }


    @Override public void updateNull(int columnIndex) throws SQLException { ro(); }
    @Override public void updateNull(String columnLabel) throws SQLException { ro(); }
    @Override public void updateBoolean(int columnIndex, boolean x) throws SQLException { ro(); }
    @Override public void updateBoolean(String columnLabel, boolean x) throws SQLException { ro(); }
    @Override public void updateByte(int columnIndex, byte x) throws SQLException { ro(); }
    @Override public void updateByte(String columnLabel, byte x) throws SQLException { ro(); }
    @Override public void updateShort(int columnIndex, short x) throws SQLException { ro(); }
    @Override public void updateShort(String columnLabel, short x) throws SQLException { ro(); }
    @Override public void updateInt(int columnIndex, int x) throws SQLException { ro(); }
    @Override public void updateInt(String columnLabel, int x) throws SQLException { ro(); }
    @Override public void updateLong(int columnIndex, long x) throws SQLException { ro(); }
    @Override public void updateLong(String columnLabel, long x) throws SQLException { ro(); }
    @Override public void updateFloat(int columnIndex, float x) throws SQLException { ro(); }
    @Override public void updateFloat(String columnLabel, float x) throws SQLException { ro(); }
    @Override public void updateDouble(int columnIndex, double x) throws SQLException { ro(); }
    @Override public void updateDouble(String columnLabel, double x) throws SQLException { ro(); }
    @Override public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException { ro(); }
    @Override public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException { ro(); }
    @Override public void updateString(int columnIndex, String x) throws SQLException { ro(); }
    @Override public void updateString(String columnLabel, String x) throws SQLException { ro(); }
    @Override public void updateBytes(int columnIndex, byte[] x) throws SQLException { ro(); }
    @Override public void updateBytes(String columnLabel, byte[] x) throws SQLException { ro(); }
    @Override public void updateDate(int columnIndex, java.sql.Date x) throws SQLException { ro(); }
    @Override public void updateDate(String columnLabel, java.sql.Date x) throws SQLException { ro(); }
    @Override public void updateTime(int columnIndex, Time x) throws SQLException { ro(); }
    @Override public void updateTime(String columnLabel, Time x) throws SQLException { ro(); }
    @Override public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException { ro(); }
    @Override public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException { ro(); }
    @Override public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException { ro(); }
    @Override public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException { ro(); }
    @Override public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException { ro(); }
    @Override public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException { ro(); }
    @Override public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException { ro(); }
    @Override public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException { ro(); }
    @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException { ro(); }
    @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException { ro(); }
    @Override public void updateObject(int columnIndex, Object x) throws SQLException { ro(); }
    @Override public void updateObject(String columnLabel, Object x) throws SQLException { ro(); }
    @Override public void insertRow() throws SQLException { ro(); }
    @Override public void updateRow() throws SQLException { ro(); }
    @Override public void deleteRow() throws SQLException { ro(); }
    @Override public void refreshRow() throws SQLException { ro(); }
    @Override public void cancelRowUpdates() throws SQLException { ro(); }
    @Override public void moveToInsertRow() throws SQLException { ro(); }
    @Override public void moveToCurrentRow() throws SQLException { ro(); }
    @Override public void updateRef(int columnIndex, Ref x) throws SQLException { ro(); }
    @Override public void updateRef(String columnLabel, Ref x) throws SQLException { ro(); }
    @Override public void updateBlob(int columnIndex, Blob x) throws SQLException { ro(); }
    @Override public void updateBlob(String columnLabel, Blob x) throws SQLException { ro(); }
    @Override public void updateClob(int columnIndex, Clob x) throws SQLException { ro(); }
    @Override public void updateClob(String columnLabel, Clob x) throws SQLException { ro(); }
    @Override public void updateArray(int columnIndex, Array x) throws SQLException { ro(); }
    @Override public void updateArray(String columnLabel, Array x) throws SQLException { ro(); }
    @Override public void updateRowId(int columnIndex, RowId x) throws SQLException { ro(); }
    @Override public void updateRowId(String columnLabel, RowId x) throws SQLException { ro(); }
    @Override public void updateNString(int columnIndex, String nString) throws SQLException { ro(); }
    @Override public void updateNString(String columnLabel, String nString) throws SQLException { ro(); }
    @Override public void updateNClob(int columnIndex, NClob nClob) throws SQLException { ro(); }
    @Override public void updateNClob(String columnLabel, NClob nClob) throws SQLException { ro(); }
    @Override public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException { ro(); }
    @Override public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException { ro(); }
    @Override public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException { ro(); }
    @Override public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException { ro(); }
    @Override public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException { ro(); }
    @Override public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException { ro(); }
    @Override public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException { ro(); }
    @Override public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException { ro(); }
    @Override public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException { ro(); }
    @Override public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException { ro(); }
    @Override public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException { ro(); }
    @Override public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException { ro(); }
    @Override public void updateClob(int columnIndex, Reader reader, long length) throws SQLException { ro(); }
    @Override public void updateClob(String columnLabel, Reader reader, long length) throws SQLException { ro(); }
    @Override public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException { ro(); }
    @Override public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException { ro(); }
    @Override public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException { ro(); }
    @Override public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException { ro(); }
    @Override public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException { ro(); }
    @Override public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException { ro(); }
    @Override public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException { ro(); }
    @Override public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException { ro(); }
    @Override public void updateCharacterStream(int columnIndex, Reader x) throws SQLException { ro(); }
    @Override public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException { ro(); }
    @Override public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException { ro(); }
    @Override public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException { ro(); }
    @Override public void updateClob(int columnIndex, Reader reader) throws SQLException { ro(); }
    @Override public void updateClob(String columnLabel, Reader reader) throws SQLException { ro(); }
    @Override public void updateNClob(int columnIndex, Reader reader) throws SQLException { ro(); }
    @Override public void updateNClob(String columnLabel, Reader reader) throws SQLException { ro(); }

    private void ro() throws SQLException {
        throw new SQLFeatureNotSupportedException("ResultSet read-only");
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
