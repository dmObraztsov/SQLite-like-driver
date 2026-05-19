package SqlParser.Antlr;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import SqlParser.QueriesStruct.WhereCondition;
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
            assertNull(q.compositePrimaryKey());
        }

        @Test
        void testCreateTableWithCompositePK() {
            String sql = "CREATE TABLE orders (orderId INTEGER, productId INTEGER, qty INTEGER, PRIMARY KEY (orderId, productId))";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.CreateTableQuery.class, query);
            Queries.CreateTableQuery q = (Queries.CreateTableQuery) query;

            assertEquals("orders", q.tableName());
            assertEquals(3, q.columns().size());
            assertNotNull(q.compositePrimaryKey());
            assertEquals(List.of("orderId", "productId"), q.compositePrimaryKey());
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

            assertInstanceOf(WhereCondition.Simple.class, q.where());
            WhereCondition.Simple where = (WhereCondition.Simple) q.where();
            assertEquals("id", where.column());
            assertEquals("=", where.op());
            assertEquals("10", where.value());
        }

        @Test
        void testSelectWithAndWhere() {
            String sql = "SELECT name FROM users WHERE age > 18 AND age < 65";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.SelectDataQuery.class, query);
            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;

            assertInstanceOf(WhereCondition.And.class, q.where());
            WhereCondition.And and = (WhereCondition.And) q.where();
            assertEquals(2, and.operands().size());
        }

        @Test
        void testSelectWithOrWhere() {
            String sql = "SELECT name FROM users WHERE id = 1 OR id = 2";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.SelectDataQuery.class, query);
            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;

            assertInstanceOf(WhereCondition.Or.class, q.where());
            WhereCondition.Or or = (WhereCondition.Or) q.where();
            assertEquals(2, or.operands().size());
        }

        @Test
        void testSelectStar() {
            QueryInterface query = parse("SELECT * FROM products");
            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;

            assertTrue(q.isStar());
            assertNull(q.selectCols());
            assertNull(q.where());
        }

        @Test
        void testSelectWithOrderByAsc() {
            String sql = "SELECT name FROM users ORDER BY name ASC";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.SelectDataQuery.class, query);
            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;

            assertEquals(1, q.orderBy().size());
            assertEquals("name", q.orderBy().get(0).col());
            assertTrue(q.orderBy().get(0).asc());
        }

        @Test
        void testSelectWithOrderByDesc() {
            String sql = "SELECT id FROM users ORDER BY id DESC";
            QueryInterface query = parse(sql);

            Queries.SelectDataQuery q = (Queries.SelectDataQuery) query;
            assertEquals(1, q.orderBy().size());
            assertEquals("id", q.orderBy().get(0).col());
            assertFalse(q.orderBy().get(0).asc());
        }

        @Test
        void testSelectGroupBy() {
            String sql = "SELECT dept, COUNT(*) FROM employees GROUP BY dept";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.GroupBySelectQuery.class, query);
            Queries.GroupBySelectQuery q = (Queries.GroupBySelectQuery) query;

            assertEquals("employees", q.tableName());
            assertEquals("dept", q.groupByCol());
            assertEquals("COUNT", q.aggFunc());
            assertNull(q.aggCol());
        }

        @Test
        void testSelectGroupByWithHaving() {
            String sql = "SELECT dept, COUNT(*) FROM employees GROUP BY dept HAVING COUNT(*) > 2";
            QueryInterface query = parse(sql);

            assertInstanceOf(Queries.GroupBySelectQuery.class, query);
            Queries.GroupBySelectQuery q = (Queries.GroupBySelectQuery) query;

            assertNotNull(q.having());
            assertInstanceOf(WhereCondition.Simple.class, q.having());
            WhereCondition.Simple having = (WhereCondition.Simple) q.having();
            assertEquals("COUNT", having.column());
            assertEquals(">", having.op());
            assertEquals("2", having.value());
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
