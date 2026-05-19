package com.simpledb.jdbc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class JdbcSmokeTest {

    private String dbName;
    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        // Уникальная БД на тест, чтобы не было пересечения с предыдущими запусками
        dbName = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        conn = DriverManager.getConnection("jdbc:simpledb:" + dbName);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
        deleteRecursively(new File("database/" + dbName));
    }

    @Test
    void driverRegisteredViaSpi() throws SQLException {
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        assertEquals(dbName, conn.getCatalog());
    }

    @Test
    void createInsertSelect() throws SQLException {
        try (Statement st = conn.createStatement()) {
            assertFalse(st.execute(
                "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)"));
            assertEquals(1, st.executeUpdate(
                "INSERT INTO users VALUES (1, \"Vasya\")"));
            assertEquals(1, st.executeUpdate(
                "INSERT INTO users VALUES (2, \"Petya\")"));
        }

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {

            ResultSetMetaData md = rs.getMetaData();
            assertEquals(2, md.getColumnCount());

            int count = 0;
            while (rs.next()) {
                assertNotNull(rs.getString("name"));
                assertTrue(rs.getInt("id") > 0);
                count++;
            }
            assertEquals(2, count);
        }
    }

    @Test
    void transactionRollback() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE t (id INTEGER PRIMARY KEY, v INTEGER)");
            st.executeUpdate("INSERT INTO t VALUES (1, 100)");
        }

        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE t SET v = 999 WHERE id = 1");
            // внутри транзакции видим новое значение
            try (ResultSet rs = st.executeQuery("SELECT v FROM t WHERE id = 1")) {
                assertTrue(rs.next());
                assertEquals(999, rs.getInt("v"));
            }
            conn.rollback();
        }
        conn.setAutoCommit(true);

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT v FROM t WHERE id = 1")) {
            assertTrue(rs.next());
            assertEquals(100, rs.getInt("v"));
        }
    }

    @Test
    void invalidSqlThrows() {
        assertThrows(SQLException.class, () -> {
            try (Statement st = conn.createStatement()) {
                st.execute("THIS IS NOT SQL");
            }
        });
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursively(c);
        }
        f.delete();
    }
}
