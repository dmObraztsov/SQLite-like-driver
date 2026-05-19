package com.simpledb.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * JDBC-драйвер
 *
 * URL формат: jdbc:simpledb:&lt;dbName&gt;
 * например: jdbc:simpledb:bank
 */
public class SimpleDriver implements Driver {

    public static final String URL_PREFIX = "jdbc:simpledb:";

    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;

    static {
        try {
            DriverManager.registerDriver(new SimpleDriver());
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось зарегистрировать SimpleDriver", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            // По контракту JDBC: если URL не наш, возвращаем null,
            // DriverManager перейдёт к следующему драйверу.
            return null;
        }
        String dbName = url.substring(URL_PREFIX.length()).trim();
        if (dbName.isEmpty()) {
            throw new SQLException("В URL не указано имя БД: " + url);
        }
        return new SimpleConnection(dbName);
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
