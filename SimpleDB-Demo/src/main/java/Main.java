import java.sql.*;

public class Main {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:simpledb:demo";

        try (Connection conn = DriverManager.getConnection(url)) {

            // Создаём таблицу
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE DATABASE IF NOT EXISTS demo");
                st.execute("USE DATABASE demo");
                st.execute("DROP TABLE IF EXISTS users");
                st.execute("""
                    CREATE TABLE users (
                        id      INTEGER PRIMARY KEY AUTOINCREMENT,
                        name    TEXT    NOT NULL,
                        email   TEXT
                    )
                """);
            }

            // Вставляем строку через PreparedStatement
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, email) VALUES (?, ?)")) {
                ps.setString(1, "Alice");
                ps.setString(2, "alice@example.com");
                ps.executeUpdate();
            }

            // Читаем обратно
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM users")) {

                ResultSetMetaData meta = rs.getMetaData();
                System.out.println("Columns: " + meta.getColumnCount());
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    System.out.printf("  [%d] %s (%s)%n", i, meta.getColumnName(i), meta.getColumnTypeName(i));
                }
                System.out.println();

                while (rs.next()) {
                    System.out.printf("id=%s  name=%s  email=%s%n",
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("email"));
                }
            }
        }
    }
}
