package Yadro.DataStruct;

import Exceptions.FileStorageException;
import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseEngineTest {

    @Mock
    private FileManager fileManager;

    private DatabaseEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine(fileManager);
    }

    @Test
    void setCurrentDatabaseDelegatesToFileManager() {
        engine.setCurrentDatabase("app_db");

        verify(fileManager).useDB("app_db");
    }

    @Test
    void createAndDropDatabaseDelegateToFileManager() throws Exception {
        engine.createDatabase("db_1");
        engine.dropDatabase("db_1");

        verify(fileManager).createDB("db_1");
        verify(fileManager).dropDB("db_1");
    }

    @Test
    void createTableCreatesStructureMetadataAndEmptyColumns() throws Exception {
        List<ColumnMetadata> columns = List.of(
                columnMeta("id", DataType.INTEGER),
                columnMeta("name", DataType.TEXT)
        );

        engine.createTable("users", columns);

        verify(fileManager).createTableStructure("users");
        verify(fileManager).saveColumnMetadata(eq("users"), eq("id"), any(ColumnMetadata.class));
        verify(fileManager).saveColumnMetadata(eq("users"), eq("name"), any(ColumnMetadata.class));

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(fileManager, times(2)).saveColumnData(eq("users"), anyString(), columnCaptor.capture());
        for (Column c : columnCaptor.getAllValues()) {
            assertTrue(c.getData().isEmpty());
        }

        ArgumentCaptor<TableMetadata> tableMetadataCaptor = ArgumentCaptor.forClass(TableMetadata.class);
        verify(fileManager).saveTableMetadata(eq("users"), tableMetadataCaptor.capture());
        TableMetadata tableMetadata = tableMetadataCaptor.getValue();
        assertEquals(2, tableMetadata.getColumnCount());
        assertEquals(List.of("id", "name"), tableMetadata.getColumnNames());
    }

    @Test
    void dropTableDelegatesToFileManager() throws Exception {
        engine.dropTable("logs");

        verify(fileManager).dropTableStructure("logs");
    }

    @Test
    void insertThrowsWhenTableDoesNotExist() {
        when(fileManager.tableExists("users")).thenReturn(false);

        Exception ex = assertThrows(Exception.class,
                () -> engine.insert("users", List.of("id"), List.of("1")));

        assertEquals("Table not found", ex.getMessage());
    }

    @Test
    void insertAddsNullForMissingColumnsAndSavesColumns() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id", "name"));

        when(fileManager.loadColumnMetadata("users", "id")).thenReturn(columnMeta("id", DataType.INTEGER));
        when(fileManager.loadColumnMetadata("users", "name")).thenReturn(columnMeta("name", DataType.TEXT));

        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf());
        when(fileManager.loadColumnData("users", "name")).thenReturn(columnOf());

        engine.insert("users", List.of("id"), List.of("42"));

        ArgumentCaptor<Column> idCaptor = ArgumentCaptor.forClass(Column.class);
        verify(fileManager).saveColumnData(eq("users"), eq("id"), idCaptor.capture());
        assertEquals(List.of("42"), idCaptor.getValue().getData());

        ArgumentCaptor<Column> nameCaptor = ArgumentCaptor.forClass(Column.class);
        verify(fileManager).saveColumnData(eq("users"), eq("name"), nameCaptor.capture());
        assertEquals(List.of("NULL"), nameCaptor.getValue().getData());
    }

    @Test
    void insertValidatesNumericTypes() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id"));
        when(fileManager.loadColumnMetadata("users", "id")).thenReturn(columnMeta("id", DataType.INTEGER));
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf());

        Exception ex = assertThrows(Exception.class,
                () -> engine.insert("users", List.of("id"), List.of("abc")));

        assertTrue(ex.getMessage().contains("Type mismatch: expected INTEGER"));
        verify(fileManager, never()).saveColumnData(anyString(), anyString(), any(Column.class));
    }

    @Test
    void selectReturnsAllRowsWhenUsingStar() throws Exception {
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id", "name"));
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf("1", "2"));
        when(fileManager.loadColumnData("users", "name")).thenReturn(columnOf("Ann", "Bob"));

        List<Row> rows = engine.select("users", null, true, null, null);

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).get("id"));
        assertEquals("Ann", rows.get(0).get("name"));
        assertEquals("2", rows.get(1).get("id"));
        assertEquals("Bob", rows.get(1).get("name"));
    }

    @Test
    void selectAppliesWhereFilter() throws Exception {
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id", "name"));
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf("1", "2"));
        when(fileManager.loadColumnData("users", "name")).thenReturn(columnOf("Ann", "Bob"));

        List<Row> rows = engine.select("users", List.of("id", "name"), false, "id", "2");

        assertEquals(1, rows.size());
        assertEquals("2", rows.get(0).get("id"));
        assertEquals("Bob", rows.get(0).get("name"));
    }

    @Test
    void joinReturnsMergedRowsWithTablePrefixes() throws Exception {
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id", "name"));
        when(fileManager.loadTableMetadata("orders")).thenReturn(tableMeta("user_id", "amount"));

        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf("1", "2"));
        when(fileManager.loadColumnData("users", "name")).thenReturn(columnOf("Ann", "Bob"));

        when(fileManager.loadColumnData("orders", "user_id")).thenReturn(columnOf("2", "1"));
        when(fileManager.loadColumnData("orders", "amount")).thenReturn(columnOf("10", "20"));

        List<Row> rows = engine.join(
                "users", List.of("name"),
                "orders", List.of("amount"),
                "id", "user_id"
        );

        assertEquals(2, rows.size());
        assertEquals("Ann", rows.get(0).get("users.name"));
        assertEquals("20", rows.get(0).get("orders.amount"));
        assertEquals("Bob", rows.get(1).get("users.name"));
        assertEquals("10", rows.get(1).get("orders.amount"));
    }

    @Test
    void beginTransactionActivatesAndSecondBeginFails() {
        engine.beginTransaction();

        assertTrue(engine.isTransactionActive());
        IllegalStateException ex = assertThrows(IllegalStateException.class, engine::beginTransaction);
        assertEquals("Transaction already active", ex.getMessage());
    }

    @Test
    void insertInsideTransactionBuffersDataWithoutImmediateSave() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id"));
        when(fileManager.loadColumnMetadata("users", "id")).thenReturn(columnMeta("id", DataType.INTEGER));
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf());

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("5"));

        verify(fileManager, never()).saveColumnData(anyString(), anyString(), any(Column.class));

        List<Row> rows = engine.select("users", null, true, null, null);
        assertEquals(1, rows.size());
        assertEquals("5", rows.get(0).get("id"));
    }

    @Test
    void rollbackClearsTransactionBufferAndState() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id"));
        when(fileManager.loadColumnMetadata("users", "id")).thenReturn(columnMeta("id", DataType.INTEGER));
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf());

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("6"));
        engine.rollback();

        assertFalse(engine.isTransactionActive());
        verify(fileManager, never()).saveColumnData(anyString(), anyString(), any(Column.class));
    }

    @Test
    void commitPersistsBufferedChangesAndEndsTransaction() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id"));
        when(fileManager.loadColumnMetadata("users", "id")).thenReturn(columnMeta("id", DataType.INTEGER));
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf());

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("7"));
        engine.commit();

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(fileManager).saveColumnData(eq("users"), eq("id"), columnCaptor.capture());
        assertEquals(List.of("7"), columnCaptor.getValue().getData());
        assertFalse(engine.isTransactionActive());
    }

    @Test
    void commitWithoutActiveTransactionFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> engine.commit());

        assertEquals("No active transaction", ex.getMessage());
    }

    @Test
    void rollbackWithoutActiveTransactionFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, engine::rollback);

        assertEquals("No active transaction", ex.getMessage());
    }

    @Test
    void commitFailureRestoresSnapshotAndThrowsReadableMessage() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMeta("id"));
        when(fileManager.loadColumnMetadata("users", "id")).thenReturn(columnMeta("id", DataType.INTEGER));

        Column diskBeforeCommit = columnOf("1");
        when(fileManager.loadColumnData("users", "id")).thenReturn(columnOf(), diskBeforeCommit);

        doThrow(new FileStorageException("disk error"))
                .doNothing()
                .when(fileManager)
                .saveColumnData(eq("users"), eq("id"), any(Column.class));

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("8"));

        FileStorageException ex = assertThrows(FileStorageException.class, () -> engine.commit());

        assertTrue(ex.getMessage().contains("Commit failed, disk state restored"));
        verify(fileManager, times(2)).saveColumnData(eq("users"), eq("id"), any(Column.class));
        assertTrue(engine.isTransactionActive());
    }

    private static ColumnMetadata columnMeta(String name, DataType type) {
        return new ColumnMetadata(name, type, 0, new ArrayList<>(), null);
    }

    private static TableMetadata tableMeta(String... names) {
        TableMetadata metadata = new TableMetadata();
        metadata.setColumnCount(names.length);
        for (String n : names) {
            metadata.addColumnName(n);
        }
        return metadata;
    }

    private static Column columnOf(String... values) {
        Column c = new Column();
        for (String value : values) {
            c.addData(c.getData().size(), value);
        }
        return c;
    }
}
