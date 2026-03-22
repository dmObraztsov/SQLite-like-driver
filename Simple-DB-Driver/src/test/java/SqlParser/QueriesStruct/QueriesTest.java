package SqlParser.QueriesStruct;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueriesTest {

    @Mock
    private DatabaseEngine engine;

    private static final String DB_NAME = "test_db";
    private static final String TABLE_NAME = "users";
    private static final String COLUMN_NAME = "age";

    @Nested
    class DatabaseOperations {

        @Test
        void testCreateDataBaseQuery() throws Exception {
            var query = new Queries.CreateDataBaseQuery(DB_NAME);

            ExecutionResult result = query.execute(engine);

            verify(engine, times(1)).createDatabase(DB_NAME);
            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains(DB_NAME));
        }

        @Test
        void testDropDataBaseQuery() throws Exception {
            var query = new Queries.DropDataBaseQuery(DB_NAME);

            ExecutionResult result = query.execute(engine);

            verify(engine, times(1)).dropDatabase(DB_NAME);
            assertTrue(result.isSuccess());
        }

        @Test
        void testUseDataBaseQuery() throws Exception {
            var query = new Queries.UseDataBaseQuery(DB_NAME);

            ExecutionResult result = query.execute(engine);

            verify(engine, times(1)).setCurrentDatabase(DB_NAME);
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    class TableBaseOperations {

        @Test
        void testCreateTableQuery() throws Exception {
            List<ColumnMetadata> columns = List.of(new ColumnMetadata());
            var query = new Queries.CreateTableQuery(TABLE_NAME, columns);

            ExecutionResult result = query.execute(engine);

            verify(engine).createTable(TABLE_NAME, columns);
            assertTrue(result.isSuccess());
        }

        @Test
        void testDropTableQuery() throws Exception {
            var query = new Queries.DropTableQuery(TABLE_NAME);

            ExecutionResult result = query.execute(engine);

            verify(engine).dropTable(TABLE_NAME);
            assertTrue(result.isSuccess());
        }

        @Test
        void testInsertTableQuery() throws Exception {
            List<String> cols = List.of("name", "age");
            List<String> vals = List.of("'Ivan'", "25");
            var query = new Queries.InsertTableQuery(TABLE_NAME, cols, vals);

            ExecutionResult result = query.execute(engine);

            verify(engine).insert(TABLE_NAME, cols, vals);
            assertTrue(result.isSuccess());
        }
    }

    @Nested
    class DataQueryOperations {

        @Test
        void testSelectDataQuery() throws Exception {
            List<Row> mockRows = List.of(new Row(Map.of("id", "1")));
            when(engine.select(TABLE_NAME, List.of("id"), false, "id", "1", false))
                    .thenReturn(mockRows);

            var query = new Queries.SelectDataQuery(List.of("id"), false, TABLE_NAME, "id", "1", false);
            ExecutionResult result = query.execute(engine);

            assertTrue(result.isSuccess());
            assertEquals(mockRows, result.getRows());
        }

        @Test
        void testJoinTableQuery() throws Exception {
            List<Row> rawMockRows = List.of(new Row(Map.of(
                    "t1.id", "1",
                    "t1.name", "test",
                    "t2.id", "1",
                    "t2.city", "NSK"
            )));
            when(engine.join("t1", List.of("id"), "t2", List.of("id"), "id", "id"))
                    .thenReturn(rawMockRows);

            var query = new Queries.JoinTableQuery("t1", List.of("id"), "t2", List.of("id"), "id", "id", false);
            ExecutionResult result = query.execute(engine);

            List<Row> expectedRows = List.of(new Row(Map.of(
                    "t1.id", "1",
                    "t2.id", "1"
            )));
            assertTrue(result.isSuccess());
            assertEquals(expectedRows, result.getRows(), "Результат должен содержать только спроецированные колонки id");
        }
    }

    @Nested
    class TransactionOperations {

        @Test
        void testBeginTransaction() {
            new Queries.BeginTransactionQuery().execute(engine);
            verify(engine).beginTransaction();
        }

        @Test
        void testCommit() throws Exception {
            new Queries.CommitQuery().execute(engine);
            verify(engine).commit();
        }

        @Test
        void testRollback() {
            new Queries.RollbackQuery().execute(engine);
            verify(engine).rollback();
        }
    }

    @Nested
    class AlterOperations {

        @Test
        void testAlterAddColumn() throws Exception {
            ColumnMetadata col = new ColumnMetadata();
            new Queries.AlterTableAddColumnQuery(TABLE_NAME, col).execute(engine);
            verify(engine).alterTableAddColumn(TABLE_NAME, col);
        }

        @Test
        void testAlterDropColumn() throws Exception {
            new Queries.AlterTableDropColumnQuery(TABLE_NAME, COLUMN_NAME).execute(engine);
            verify(engine).alterTableDropColumn(TABLE_NAME, COLUMN_NAME);
        }

        @Test
        void testAlterRenameColumn() throws Exception {
            new Queries.AlterTableRenameColumnQuery(TABLE_NAME, "oldName", "newName").execute(engine);
            verify(engine).alterTableRenameColumn(TABLE_NAME, "oldName", "newName");
        }

        @Test
        void testAlterRenameTable() throws Exception {
            new Queries.AlterTableRenameTableQuery(TABLE_NAME, "newTable").execute(engine);
            verify(engine).alterTableRenameTable(TABLE_NAME, "newTable");
        }
    }

    @Nested
    class DataManipulationOperations {

        @Test
        void testDeleteRowsFound() throws Exception {
            when(engine.delete(TABLE_NAME, "id", "1")).thenReturn(5);

            var query = new Queries.DeleteTableQuery(TABLE_NAME, "id", "1");
            ExecutionResult result = query.execute(engine);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("5 row(s) deleted"));
        }

        @Test
        void testDeleteNoRowsFound() throws Exception {
            when(engine.delete(TABLE_NAME, "id", "1")).thenReturn(0);

            var query = new Queries.DeleteTableQuery(TABLE_NAME, "id", "1");
            ExecutionResult result = query.execute(engine);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("No matching rows found"));
        }

        @Test
        void testUpdateRowsFound() throws Exception {
            Map<String, String> values = Map.of("name", "Oleg");
            when(engine.update(TABLE_NAME, values, "id", "1")).thenReturn(2);

            var query = new Queries.UpdateTableQuery(TABLE_NAME, values, "id", "1");
            ExecutionResult result = query.execute(engine);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("2 row(s) updated"));
        }

        @Test
        void testUpdateNoRowsFound() throws Exception {
            Map<String, String> values = Map.of("name", "Oleg");
            when(engine.update(TABLE_NAME, values, "id", "1")).thenReturn(0);

            var query = new Queries.UpdateTableQuery(TABLE_NAME, values, "id", "1");
            ExecutionResult result = query.execute(engine);

            assertTrue(result.getMessage().contains("No matching rows found"));
        }
    }

    @Nested
    class ExceptionHandling {

        @Test
        void shouldPropagateException_WhenEngineThrows() throws Exception {
            doThrow(new FileStorageException("Disk full")).when(engine).dropDatabase(DB_NAME);

            var query = new Queries.DropDataBaseQuery(DB_NAME);

            assertThrows(FileStorageException.class, () -> query.execute(engine));
        }
    }
}