package com.simpledb.jdbc;

import Exceptions.AlreadyExistsException;
import Exceptions.FileStorageException;
import FileWork.Binary.BinaryFileStorage;
import FileWork.FileManager;
import Yadro.DataStruct.DatabaseEngine;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class SimpleConnection implements Connection {

    private final DatabaseEngine engine;
    private final String dbName;
    private boolean autoCommit = true;
    private boolean closed = false;
    private boolean readOnly = false;

    public SimpleConnection(String dbName) throws SQLException {
        this.dbName = dbName;
        BinaryFileStorage storage = new BinaryFileStorage();
        FileManager fileManager = new FileManager(storage);
        this.engine = new DatabaseEngine(fileManager);
        try {
            try {
                engine.createDatabase(dbName);
            } catch (AlreadyExistsException ignored) {

            } catch (FileStorageException e) {

            }
            engine.setCurrentDatabase(dbName);
        } catch (FileStorageException e) {
            throw new SQLException("Не удалось открыть БД '" + dbName + "': " + e.getMessage(), e);
        }
    }

   
    DatabaseEngine getEngine() {
        return engine;
    }

   
    void ensureTransactionIfNeeded() {
        if (!autoCommit && !engine.isTransactionActive()) {
            engine.beginTransaction();
        }
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        if (this.autoCommit == autoCommit) return;

        if (autoCommit && engine.isTransactionActive()) {
            try {
                engine.commit();
            } catch (Exception e) {
                throw new SQLException("Commit при смене autoCommit упал: " + e.getMessage(), e);
            }
        }
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkClosed();
        return autoCommit;
    }

    @Override
    public void commit() throws SQLException {
        checkClosed();
        if (autoCommit) {
            throw new SQLException("Нельзя звать commit() при autoCommit=true");
        }
        if (!engine.isTransactionActive()) {
            return;
        }
        try {
            engine.commit();
        } catch (Exception e) {
            throw new SQLException("Commit упал: " + e.getMessage(), e);
        }
    }

    @Override
    public void rollback() throws SQLException {
        checkClosed();
        if (autoCommit) {
            throw new SQLException("Нельзя звать rollback() при autoCommit=true");
        }
        if (engine.isTransactionActive()) {
            engine.rollback();
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkClosed();
        return new SimpleStatement(this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return createStatement();
    }

    @Override
    public void close() throws SQLException {
        if (closed) return;

        if (!autoCommit && engine.isTransactionActive()) {
            engine.rollback();
        }
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isValid(int timeout) {
        return !closed;
    }

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("Connection закрыт");
    }

    @Override
    public String getCatalog() {
        return dbName;
    }

    @Override
    public void setCatalog(String catalog) {
    }

    @Override
    public String getSchema() {
        return null;
    }

    @Override
    public void setSchema(String schema) {}

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkClosed();
        return new SimpleDatabaseMetaData(this, "jdbc:simpledb:" + dbName);
    }

    @Override
    public boolean isReadOnly() { return readOnly; }

    @Override
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

    @Override
    public void setTransactionIsolation(int level) {

    }

    @Override
    public int getTransactionIsolation() {
        return Connection.TRANSACTION_READ_COMMITTED;
    }

    @Override
    public SQLWarning getWarnings() {
        return null;
    }

    @Override
    public void clearWarnings() {}

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkClosed();
        return new SimplePreparedStatement(this, sql);
    }

    @Override public PreparedStatement prepareStatement(String sql, int a, int b) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int a, int b, int c) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return prepareStatement(sql); }
    @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return prepareStatement(sql); }
    @Override public CallableStatement prepareCall(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public CallableStatement prepareCall(String sql, int a, int b) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public CallableStatement prepareCall(String sql, int a, int b, int c) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public String nativeSQL(String sql) { return sql; }
    @Override public Savepoint setSavepoint() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Savepoint setSavepoint(String name) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void rollback(Savepoint savepoint) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setHoldability(int holdability) { }
    @Override public int getHoldability() { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public Map<String, Class<?>> getTypeMap() { return Map.of(); }
    @Override public void setTypeMap(Map<String, Class<?>> map) { }
    @Override public Clob createClob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Blob createBlob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public NClob createNClob() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public SQLXML createSQLXML() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Properties getClientInfo() { return new Properties(); }
    @Override public String getClientInfo(String name) { return null; }
    @Override public void setClientInfo(String name, String value) { }
    @Override public void setClientInfo(Properties properties) { }
    @Override public void abort(Executor executor) { closed = true; }
    @Override public void setNetworkTimeout(Executor executor, int milliseconds) { }
    @Override public int getNetworkTimeout() { return 0; }

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
