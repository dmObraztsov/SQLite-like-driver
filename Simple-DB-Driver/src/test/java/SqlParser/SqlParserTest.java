package SqlParser;

import SqlParser.Antlr.SQLProcessor;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование SQL Парсера и Процессора")
class SqlParserTest {

    @Test
    @DisplayName("Парсинг CREATE DATABASE")
    void testCreateDatabaseParsing() {
        String sql = "CREATE DATABASE my_database";
        QueryInterface query = SQLProcessor.getQuery(sql);

        assertNotNull(query);
        assertInstanceOf(Queries.CreateDataBaseQuery.class, query);
    }

    @Test
    @DisplayName("Парсинг CREATE TABLE с типами данных")
    void testCreateTableParsing() {
        String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)";
        QueryInterface query = SQLProcessor.getQuery(sql);

        assertNotNull(query, "Запрос не должен быть null");
        assertInstanceOf(Queries.CreateTableQuery.class, query);
    }

    @Test
    @DisplayName("Парсинг SELECT с условием WHERE")
    void testSelectParsing() {
        String sql = "SELECT name, id FROM users WHERE id = 10";
        QueryInterface query = SQLProcessor.getQuery(sql);

        assertNotNull(query);
        assertInstanceOf(Queries.SelectDataQuery.class, query);
    }

    @Test
    @DisplayName("Парсинг INSERT INTO")
    void testInsertParsing() {
        String sql = "INSERT INTO users (id, name) VALUES (1, \"Ivan\")";
        QueryInterface query = SQLProcessor.getQuery(sql);

        assertNotNull(query);
        assertInstanceOf(Queries.InsertTableQuery.class, query);
    }

    @Test
    @DisplayName("Обработка синтаксической ошибки (Invalid SQL)")
    void testSyntaxError() {
        String sql = "MAKE DATABASE test"; // Некорректное ключевое слово
        QueryInterface query = SQLProcessor.getQuery(sql);

        assertNull(query, "Процессор должен возвращать null при синтаксических ошибках");
    }

    @Test
    @DisplayName("Парсинг ALTER TABLE ADD COLUMN")
    void testAlterTableParsing() {
        String sql = "ALTER TABLE users ADD COLUMN age INTEGER";
        QueryInterface query = SQLProcessor.getQuery(sql);

        assertNotNull(query);
    }
}