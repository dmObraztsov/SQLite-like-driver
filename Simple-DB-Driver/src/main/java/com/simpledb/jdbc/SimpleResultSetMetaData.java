package com.simpledb.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class SimpleResultSetMetaData implements ResultSetMetaData {

    private final List<String> columnNames;
    private final List<Integer> columnTypes;

    SimpleResultSetMetaData(List<String> columnNames, List<Integer> columnTypes) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
    }

    @Override public int getColumnCount()  { return columnNames.size(); }
    @Override public String getColumnLabel(int column) { return columnNames.get(column - 1); }
    @Override public String getColumnName(int column)  { return columnNames.get(column - 1); }

    @Override
    public int getColumnType(int column) {
        return columnTypes.get(column - 1);
    }

    @Override
    public String getColumnTypeName(int column) {
        return switch (columnTypes.get(column - 1)) {
            case Types.INTEGER   -> "INTEGER";
            case Types.BIGINT    -> "BIGINT";
            case Types.DOUBLE    -> "DOUBLE";
            case Types.DATE      -> "DATE";
            case Types.TIMESTAMP -> "TIMESTAMP";
            default              -> "VARCHAR";
        };
    }

    @Override
    public String getColumnClassName(int column) {
        return switch (columnTypes.get(column - 1)) {
            case Types.INTEGER   -> Integer.class.getName();
            case Types.BIGINT    -> Long.class.getName();
            case Types.DOUBLE    -> Double.class.getName();
            case Types.DATE      -> java.sql.Date.class.getName();
            case Types.TIMESTAMP -> java.sql.Timestamp.class.getName();
            default              -> String.class.getName();
        };
    }

    @Override
    public boolean isSigned(int column) {
        int t = columnTypes.get(column - 1);
        return t == Types.INTEGER || t == Types.BIGINT || t == Types.DOUBLE;
    }

    @Override public int getColumnDisplaySize(int column) { return 50; }
    @Override public int getPrecision(int column) { return 0; }
    @Override public int getScale(int column) { return 0; }
    @Override public int isNullable(int column) { return columnNullable; }
    @Override public boolean isAutoIncrement(int column) { return false; }
    @Override public boolean isCaseSensitive(int column) { return true; }
    @Override public boolean isSearchable(int column) { return true; }
    @Override public boolean isCurrency(int column) { return false; }
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
