package SqlParser.Antlr;

import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SQLProcessorTest {

    @Test
    void shouldReturnQueryForValidSql() {
        String sql = "CREATE DATABASE sales_db";

        QueryInterface result = SQLProcessor.getQuery(sql);

        assertNotNull(result, "Результат не должен быть null для валидного SQL");
        assertInstanceOf(Queries.CreateDataBaseQuery.class, result);

        Queries.CreateDataBaseQuery createQuery = (Queries.CreateDataBaseQuery) result;
        assertEquals("sales_db", createQuery.databaseName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE DATABASE",
            "SELECT FROM",
            "INSERT INTO table (id",
            "NOT A SQL COMMAND",
            ""
    })
    void shouldReturnNullForInvalidSql(String invalidSql) {
        QueryInterface result = SQLProcessor.getQuery(invalidSql);

        assertNull(result, "Для некорректного SQL: '" + invalidSql + "' должен возвращаться null");
    }

    @Test
    void shouldHandleDifferentCase() {
        QueryInterface result = SQLProcessor.getQuery("select * from users");
        assertNotNull(result);
        assertInstanceOf(Queries.SelectDataQuery.class, result);
    }
}