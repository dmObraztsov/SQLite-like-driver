package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import Yadro.DataStruct.DataType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AntlrParserTest {

    private QueryInterface parse(String sql) {
        SQLLexer lexer = new SQLLexer(CharStreams.fromString(sql));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SQLParser parser = new SQLParser(tokens);

        SQLParser.QueryContext tree = parser.query();
        AntlrParser visitor = new AntlrParser();
        return visitor.visit(tree);
    }

    @Nested
    class DatabaseTests {
        @Test
        void testCreateDatabase() {
            QueryInterface query = parse("CREATE DATABASE my_db");

            assertInstanceOf(Queries.CreateDataBaseQuery.class, query);
            Queries.CreateDataBaseQuery q = (Queries.CreateDataBaseQuery) query;
            assertEquals("my_db", q.databaseName());
        }

        @Test
        void testDropDatabase() {
            QueryInterface query = parse("DROP DATABASE old_db");
            assertInstanceOf(Queries.DropDataBaseQuery.class, query);
            assertEquals("old_db", ((Queries.DropDataBaseQuery) query).databaseName());
        }
    }

    @Nested
    class TableTests {
        @Test
        void testCreateTable() {
            String sql = "CREATE TABLE users (id INTEGER, name TEXT)";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.CreateTableQuery.class, query);
            Queries.CreateTableQuery q = (Queries.CreateTableQuery) query;

            assertEquals("users", q.tableName());
            assertEquals(2, q.columns().size());
            assertEquals(DataType.INTEGER, q.columns().get(0).getType());
            assertEquals("name", q.columns().get(1).getName());
        }
    }

    @Nested
    class DataManipulationTests {
        @Test
        void testInsert() {
            String sql = "INSERT INTO users (id, name) VALUES (1, \"Alice\")";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.InsertTableQuery.class, query);
            Queries.InsertTableQuery q = (Queries.InsertTableQuery) query;

            assertEquals("users", q.tableName());
            assertEquals(List.of("id", "name"), q.columnNames());
            assertEquals(List.of("1", "Alice"), q.values());
        }

        @Test
        void testSelectWithWhere() {
            String sql = "SELECT name FROM users WHERE id = 10";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.SelectDataQuery.class, query);
            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;

            assertEquals("users", q.tableName());
            assertEquals(List.of("name"), q.selectCols());
            assertFalse(q.isStar());
            assertEquals("id", q.whereName());
            assertEquals("10", q.whereValue());
        }

        @Test
        void testSelectStar() {
            QueryInterface query = parse("SELECT * FROM products");
            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;

            assertTrue(q.isStar());
            assertNull(q.selectCols());
        }
    }

    @Nested
    class AlterTests {
        @Test
        void testAlterAddColumn() {
            QueryInterface query = parse("ALTER TABLE users ADD COLUMN age INTEGER");
            assertInstanceOf(Queries.AlterTableAddColumnQuery.class, query);

            Queries.AlterTableAddColumnQuery q = (Queries.AlterTableAddColumnQuery) query;
            assertEquals("age", q.column().getName());
        }

        @Test
        void testAlterRenameTable() {
            QueryInterface query = parse("ALTER TABLE users RENAME TO clients");
            assertInstanceOf(Queries.AlterTableRenameTableQuery.class, query);
            assertEquals("clients", ((Queries.AlterTableRenameTableQuery) query).newName());
        }
    }

    @Nested
    class TransactionTests {
        @Test
        void testCommit() {
            assertInstanceOf(Queries.CommitQuery.class, parse("COMMIT"));
        }

        @Test
        void testRollback() {
            assertInstanceOf(Queries.RollbackQuery.class, parse("ROLLBACK"));
        }
    }
}