package SqlParser.Antlr;

import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.DataType;
import Yadro.DataStruct.Constraints;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SQLProcessorTest {

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    @SuppressWarnings("unchecked")
    private List<ColumnMetadata> getTableColumns(Queries.CreateTableQuery query) throws Exception {
        Field columnsField = Queries.CreateTableQuery.class.getDeclaredField("tableColumns");
        columnsField.setAccessible(true);
        return (List<ColumnMetadata>) columnsField.get(query);
    }

    private String getTableName(Queries.CreateTableQuery query) throws Exception {
        Field tableNameField = Queries.CreateTableQuery.class.getDeclaredField("tableName");
        tableNameField.setAccessible(true);
        return (String) tableNameField.get(query);
    }

    @SuppressWarnings("unchecked")
    private List<String> getInsertColumns(Queries.InsertTableQuery query) throws Exception {
        Field columnsField = Queries.InsertTableQuery.class.getDeclaredField("columns");
        columnsField.setAccessible(true);
        return (List<String>) columnsField.get(query);
    }

    @SuppressWarnings("unchecked")
    private List<String> getInsertValues(Queries.InsertTableQuery query) throws Exception {
        Field valuesField = Queries.InsertTableQuery.class.getDeclaredField("values");
        valuesField.setAccessible(true);
        return (List<String>) valuesField.get(query);
    }

    private ColumnMetadata getAlterAddColumn(Queries.AlterTableQuery.AlterAddColumnQuery query) throws Exception {
        Field columnField = Queries.AlterTableQuery.AlterAddColumnQuery.class.getDeclaredField("column");
        columnField.setAccessible(true);
        return (ColumnMetadata) columnField.get(query);
    }

    // ========== ТЕСТЫ ==========

    @Test
    @DisplayName("Should parse CREATE TABLE with columns")
    void shouldParseCreateTableWithColumns() throws Exception {
        // Given
        String sql = "CREATE TABLE users (id INTEGER, name TEXT, age INTEGER)";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.CreateTableQuery.class, result);

        Queries.CreateTableQuery query = (Queries.CreateTableQuery) result;

        String tableName = getTableName(query);
        List<ColumnMetadata> columns = getTableColumns(query);

        assertEquals("users", tableName);
        assertNotNull(columns);
        assertEquals(3, columns.size());

        // Проверяем колонки
        assertEquals("id", columns.get(0).getName());
        assertEquals(DataType.INTEGER, columns.get(0).getType());

        assertEquals("name", columns.get(1).getName());
        assertEquals(DataType.TEXT, columns.get(1).getType());

        assertEquals("age", columns.get(2).getName());
        assertEquals(DataType.INTEGER, columns.get(2).getType());
    }

    @Test
    @DisplayName("Should parse CREATE TABLE with constraints")
    void shouldParseCreateTableWithConstraints() throws Exception {
        // Given
        String sql = "CREATE TABLE users (" +
                "id INTEGER PRIMARYKEY NOTNULL AUTOINCREMENT, " +
                "email TEXT UNIQUE NOTNULL" +
                ")";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        Queries.CreateTableQuery query = (Queries.CreateTableQuery) result;

        List<ColumnMetadata> columns = getTableColumns(query);
        assertEquals(2, columns.size());

        // Проверяем констрейнты первой колонки
        List<Constraints> idConstraints = columns.getFirst().getConstraints();
        assertTrue(idConstraints.contains(Constraints.PRIMARY_KEY));
        assertTrue(idConstraints.contains(Constraints.NOT_NULL));
        assertTrue(idConstraints.contains(Constraints.AUTOINCREMENT));

        // Проверяем констрейнты второй колонки
        List<Constraints> emailConstraints = columns.get(1).getConstraints();
        assertTrue(emailConstraints.contains(Constraints.UNIQUE));
        assertTrue(emailConstraints.contains(Constraints.NOT_NULL));
    }

    @Test
    @DisplayName("Should parse ALTER TABLE ADD COLUMN")
    void shouldParseAlterTableAddColumn() throws Exception {
        // Given
        String sql = "ALTER TABLE users ADD COLUMN email TEXT UNIQUE NOTNULL";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.AlterTableQuery.AlterAddColumnQuery.class, result);

        Queries.AlterTableQuery.AlterAddColumnQuery query =
                (Queries.AlterTableQuery.AlterAddColumnQuery) result;

        ColumnMetadata column = getAlterAddColumn(query);
        assertNotNull(column);
        assertEquals("email", column.getName());
        assertEquals(DataType.TEXT, column.getType());

        List<Constraints> constraints = column.getConstraints();
        assertTrue(constraints.contains(Constraints.UNIQUE));
        assertTrue(constraints.contains(Constraints.NOT_NULL));
    }

    @Test
    @DisplayName("Should parse INSERT statement with columns and values")
    void shouldParseInsertStatement() throws Exception {
        // Given
        String sql = "INSERT INTO users (id, name, email) VALUES (1, 'John', 'john@example.com')";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.InsertTableQuery.class, result);

        Queries.InsertTableQuery query = (Queries.InsertTableQuery) result;

        List<String> columns = getInsertColumns(query);
        List<String> values = getInsertValues(query);

        assertNotNull(columns);
        assertNotNull(values);

        assertEquals(3, columns.size());
        assertEquals(3, values.size());

        assertEquals("id", columns.get(0));
        assertEquals("name", columns.get(1));
        assertEquals("email", columns.get(2));

        assertEquals("1", values.get(0));
        assertEquals("'John'", values.get(1));
        assertEquals("'john@example.com'", values.get(2));
    }

    @Test
    @DisplayName("Should parse INSERT statement without columns")
    void shouldParseInsertWithoutColumns() throws Exception {
        // Given
        String sql = "INSERT INTO users VALUES (1, 'John', 'john@example.com')";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.InsertTableQuery.class, result);

        Queries.InsertTableQuery query = (Queries.InsertTableQuery) result;

        List<String> columns = getInsertColumns(query);
        List<String> values = getInsertValues(query);

        assertTrue(columns.isEmpty());
        assertEquals(3, values.size());
    }

    @Test
    @DisplayName("Should parse ALTER TABLE RENAME")
    void shouldParseAlterTableRename() throws Exception {
        // Given
        String sql = "ALTER TABLE old_name RENAME TO new_name";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.AlterTableQuery.AlterRenameTableQuery.class, result);

        Queries.AlterTableQuery.AlterRenameTableQuery query =
                (Queries.AlterTableQuery.AlterRenameTableQuery) result;

        // Проверяем через рефлексию
        Field changeTableNameField = Queries.AlterTableQuery.AlterRenameTableQuery.class
                .getDeclaredField("changeTableName");
        changeTableNameField.setAccessible(true);
        String newTableName = (String) changeTableNameField.get(query);

        assertEquals("new_name", newTableName);
    }

    @Test
    @DisplayName("Should parse ALTER TABLE DROP COLUMN")
    void shouldParseAlterTableDropColumn() throws Exception {
        // Given
        String sql = "ALTER TABLE users DROP COLUMN email";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.AlterTableQuery.AlterDropColumnQuery.class, result);

        Queries.AlterTableQuery.AlterDropColumnQuery query =
                (Queries.AlterTableQuery.AlterDropColumnQuery) result;

        Field dropColumnNameField = Queries.AlterTableQuery.AlterDropColumnQuery.class
                .getDeclaredField("dropColumnName");
        dropColumnNameField.setAccessible(true);
        String columnName = (String) dropColumnNameField.get(query);

        assertEquals("email", columnName);
    }

    @Test
    @DisplayName("Should parse all data types correctly")
    void shouldParseAllDataTypes() throws Exception {
        // Given
        String sql = "CREATE TABLE test (col1 INTEGER, col2 REAL, col3 TEXT)";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        Queries.CreateTableQuery query = (Queries.CreateTableQuery) result;

        List<ColumnMetadata> columns = getTableColumns(query);

        assertEquals(DataType.INTEGER, columns.get(0).getType());
        assertEquals(DataType.REAL, columns.get(1).getType());
        assertEquals(DataType.TEXT, columns.get(2).getType());
    }

    @Test
    @DisplayName("Should return null for invalid SQL")
    void shouldReturnNullForInvalidSql() {
        // Given
        String sql = "CREATE DATABSE test"; // Опечатка

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle SQL with extra whitespace")
    void shouldHandleExtraWhitespace() throws Exception {
        // Given
        String sql = "  CREATE   TABLE   users   (   id   INTEGER   )  ";

        // When
        QueryInterface result = SQLProcessor.getQuery(sql);

        // Then
        assertNotNull(result);
        assertInstanceOf(Queries.CreateTableQuery.class, result);

        Queries.CreateTableQuery query = (Queries.CreateTableQuery) result;
        List<ColumnMetadata> columns = getTableColumns(query);

        assertEquals(1, columns.size());
        assertEquals("id", columns.getFirst().getName());
    }
}