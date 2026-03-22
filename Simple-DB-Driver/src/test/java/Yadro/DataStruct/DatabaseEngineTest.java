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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseEngineTest {

    @Mock
    FileManager fileManager;

    DatabaseEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine(fileManager);
    }

    @Test
    void setCurrentDatabaseShouldDelegate() {
        try {
            engine.setCurrentDatabase("shop");
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
        try {
            verify(fileManager).useDB("shop");
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createAndDropDatabaseShouldDelegate() throws Exception {
        engine.createDatabase("shop");
        engine.dropDatabase("shop");

        verify(fileManager).createDB("shop");
        verify(fileManager).dropDB("shop");
    }

    @Test
    void createTableShouldCreateStructureAndPersistColumns() throws Exception {
        List<ColumnMetadata> columns = List.of(
                new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null),
                new ColumnMetadata("name", DataType.TEXT, 0, new ArrayList<>(), null)
        );

        engine.createTable("users", columns);

        verify(fileManager).createTableStructure("users");
        verify(fileManager).saveColumnMetadata(eq("users"), eq("id"), any(ColumnMetadata.class));
        verify(fileManager).saveColumnMetadata(eq("users"), eq("name"), any(ColumnMetadata.class));
        verify(fileManager, times(2)).saveColumnData(eq("users"), anyString(), any(Column.class));
        verify(fileManager).saveTableMetadata(eq("users"), any(TableMetadata.class));
    }

    @Test
    void dropTableShouldDelegate() throws Exception {
        engine.dropTable("users");
        verify(fileManager).dropTableStructure("users");
    }

    @Test
    void insertShouldThrowWhenTableDoesNotExist() {
        when(fileManager.tableExists("users")).thenReturn(false);

        assertThatThrownBy(() -> engine.insert("users", List.of("id"), List.of("1")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Table not found");
    }

    @Test
    void insertShouldThrowWhenIntegerTypeMismatch() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);
        when(fileManager.loadColumnMetadata("users", "id"))
                .thenReturn(new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null));
        when(fileManager.loadColumnData("users", "id")).thenReturn(new Column());

        assertThatThrownBy(() -> engine.insert("users", List.of("id"), List.of("oops")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Type mismatch: expected INTEGER");
    }

    @Test
    void insertShouldAcceptRealValues() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("score");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);
        when(fileManager.loadColumnMetadata("users", "score"))
                .thenReturn(new ColumnMetadata("score", DataType.REAL, 0, new ArrayList<>(), null));
        when(fileManager.loadColumnData("users", "score")).thenReturn(new Column());

        engine.insert("users", List.of("score"), List.of("3.14"));

        verify(fileManager).saveColumnData(eq("users"), eq("score"), any(Column.class));
    }

    @Test
    void insertShouldPersistAllColumnsAndUseNullForMissingOnes() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        tableMetadata.addColumnName("name");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);

        when(fileManager.loadColumnMetadata("users", "id"))
                .thenReturn(new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null));
        when(fileManager.loadColumnMetadata("users", "name"))
                .thenReturn(new ColumnMetadata("name", DataType.TEXT, 0, new ArrayList<>(), null));

        when(fileManager.loadColumnData("users", "id")).thenReturn(new Column());
        when(fileManager.loadColumnData("users", "name")).thenReturn(new Column());

        engine.insert("users", List.of("id"), List.of("42"));

        verify(fileManager, times(2)).saveColumnData(eq("users"), anyString(), any(Column.class));
    }

    @Test
    void insertInTransactionShouldNotWriteToDiskUntilCommit() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);
        when(fileManager.loadColumnMetadata("users", "id"))
                .thenReturn(new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null));
        when(fileManager.loadColumnData("users", "id")).thenReturn(new Column());

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("1"));

        verify(fileManager, never()).saveColumnData(eq("users"), eq("id"), any(Column.class));

        engine.commit();

        verify(fileManager, atLeastOnce()).saveColumnData(eq("users"), eq("id"), any(Column.class));
    }

    @Test
    void selectWithStarShouldReturnAllRows() throws Exception {
        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        tableMetadata.addColumnName("name");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);

        Column ids = new Column();
        ids.setData(new ArrayList<>(List.of("1", "2")));
        Column names = new Column();
        names.setData(new ArrayList<>(List.of("Ann", "Bob")));

        when(fileManager.loadColumnData("users", "id")).thenReturn(ids);
        when(fileManager.loadColumnData("users", "name")).thenReturn(names);

        List<Row> rows = engine.select("users", null, true, null, null, false);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("id")).isEqualTo("1");
        assertThat(rows.get(0).get("name")).isEqualTo("Ann");
        assertThat(rows.get(1).get("id")).isEqualTo("2");
    }

    @Test
    void selectWithWhereShouldFilterRows() throws Exception {
        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        tableMetadata.addColumnName("name");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);

        Column ids = new Column();
        ids.setData(new ArrayList<>(List.of("1", "2")));
        Column names = new Column();
        names.setData(new ArrayList<>(List.of("Ann", "Bob")));

        when(fileManager.loadColumnData("users", "id")).thenReturn(ids);
        when(fileManager.loadColumnData("users", "name")).thenReturn(names);

        List<Row> rows = engine.select("users", List.of("id", "name"), false, "id", "2", false);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("name")).isEqualTo("Bob");
    }

    @Test
    void joinShouldReturnPrefixedColumnsForMatchingRows() throws Exception {
        TableMetadata leftMeta = new TableMetadata();
        leftMeta.addColumnName("id");
        leftMeta.addColumnName("name");

        TableMetadata rightMeta = new TableMetadata();
        rightMeta.addColumnName("userId");
        rightMeta.addColumnName("city");

        when(fileManager.loadTableMetadata("users")).thenReturn(leftMeta);
        when(fileManager.loadTableMetadata("profiles")).thenReturn(rightMeta);

        Column userIds = new Column();
        userIds.setData(new ArrayList<>(List.of("1", "2")));
        Column userNames = new Column();
        userNames.setData(new ArrayList<>(List.of("Ann", "Bob")));
        when(fileManager.loadColumnData("users", "id")).thenReturn(userIds);
        when(fileManager.loadColumnData("users", "name")).thenReturn(userNames);

        Column profileIds = new Column();
        profileIds.setData(new ArrayList<>(List.of("2", "3")));
        Column cities = new Column();
        cities.setData(new ArrayList<>(List.of("Paris", "Rome")));
        when(fileManager.loadColumnData("profiles", "userId")).thenReturn(profileIds);
        when(fileManager.loadColumnData("profiles", "city")).thenReturn(cities);

        List<Row> rows = engine.join("users", List.of("id"), "profiles", List.of("city"), "id", "userId");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("users.id")).isEqualTo("2");
        assertThat(rows.get(0).get("profiles.city")).isEqualTo("Paris");
    }

    @Test
    void beginTransactionTwiceShouldThrow() {
        engine.beginTransaction();
        assertThatThrownBy(() -> engine.beginTransaction())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction already active");
    }

    @Test
    void transactionLifecycleShouldToggleState() {
        engine.beginTransaction();
        assertThat(engine.isTransactionActive()).isTrue();

        engine.rollback();
        assertThat(engine.isTransactionActive()).isFalse();
    }

    @Test
    void rollbackWithoutTransactionShouldThrow() {
        assertThatThrownBy(() -> engine.rollback())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active transaction");
    }

    @Test
    void commitWithoutTransactionShouldThrow() {
        assertThatThrownBy(() -> engine.commit())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active transaction");
    }

    @Test
    void commitShouldWriteBufferedChanges() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);
        when(fileManager.loadColumnMetadata("users", "id"))
                .thenReturn(new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null));

        Column current = new Column();
        current.getData().add("1");
        when(fileManager.loadColumnData("users", "id")).thenReturn(current);

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("2"));
        engine.commit();

        verify(fileManager, atLeastOnce()).saveColumnData(eq("users"), eq("id"), any(Column.class));
        assertThat(engine.isTransactionActive()).isFalse();
    }

    @Test
    void commitShouldWriteBufferedCopyNotOriginalColumnInstance() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);
        when(fileManager.loadColumnMetadata("users", "id"))
                .thenReturn(new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null));

        Column original = new Column();
        original.getData().add("1");
        when(fileManager.loadColumnData("users", "id")).thenReturn(original);

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("2"));
        engine.commit();

        ArgumentCaptor<Column> columnCaptor = ArgumentCaptor.forClass(Column.class);
        verify(fileManager, atLeastOnce()).saveColumnData(eq("users"), eq("id"), columnCaptor.capture());

        Column persisted = columnCaptor.getValue();
        assertThat(persisted).isNotSameAs(original);
        assertThat(persisted.getData()).containsExactly("1", "2");
    }

    @Test
    void commitShouldRestoreSnapshotAndThrowWhenWriteFails() throws Exception {
        when(fileManager.tableExists("users")).thenReturn(true);

        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.addColumnName("id");
        when(fileManager.loadTableMetadata("users")).thenReturn(tableMetadata);
        when(fileManager.loadColumnMetadata("users", "id"))
                .thenReturn(new ColumnMetadata("id", DataType.INTEGER, 0, new ArrayList<>(), null));

        Column current = new Column();
        current.getData().add("1");
        when(fileManager.loadColumnData("users", "id")).thenReturn(current);

        doThrow(new FileStorageException("disk error"))
                .when(fileManager).saveColumnData(eq("users"), eq("id"), any(Column.class));

        engine.beginTransaction();
        engine.insert("users", List.of("id"), List.of("2"));

        assertThatThrownBy(() -> engine.commit())
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Commit failed");
    }
}
