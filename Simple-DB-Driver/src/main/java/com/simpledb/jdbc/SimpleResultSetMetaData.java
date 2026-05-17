package com.simpledb.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * Описание колонок результата. У нашего движка типов как таковых нет — всё String,
 * поэтому отдаём Types.VARCHAR для всех колонок. Для агрегатов (COUNT/SUM/AVG/MIN/MAX)
 * имена колонок задаются движком, и они уже информативны.
 */
public class SimpleResultSetMetaData implements ResultSetMetaData {

    private final List<String> columnNames;

    SimpleResultSetMetaData(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    @Override public int getColumnCount() { return columnNames.size(); }
    @Override public String getColumnLabel(int column) { return columnNames.get(column - 1); }
    @Override public String getColumnName(int column) { return columnNames.get(column - 1); }
    @Override public int getColumnType(int column) { return Types.VARCHAR; }
    @Override public String getColumnTypeName(int column) { return "VARCHAR"; }
    @Override public String getColumnClassName(int column) { return String.class.getName(); }
    @Override public int getColumnDisplaySize(int column) { return 50; }
    @Override public int getPrecision(int column) { return 0; }
    @Override public int getScale(int column) { return 0; }
    @Override public int isNullable(int column) { return columnNullable; }
    @Override public boolean isAutoIncrement(int column) { return false; }
    @Override public boolean isCaseSensitive(int column) { return true; }
    @Override public boolean isSearchable(int column) { return true; }
    @Override public boolean isCurrency(int column) { return false; }
    @Override public boolean isSigned(int column) { return false; }
    @Override public boolean isReadOnly(int column) { return true; }
    @Override public boolean isWritable(int column) { return false; }
    @Override public boolean isDefinitelyWritable(int column) { return false; }
    @Override public String getSchemaName(int column) { return ""; }
    @Override public String getCatalogName(int column) { return ""; }
    @Override public String getTableName(int column) { return ""; }

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
