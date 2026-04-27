package FileWork.Metadata;

import Yadro.DataStruct.Collate;
import Yadro.DataStruct.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableMetadataTest {

    private TableMetadata tableMetadata;

    @BeforeEach
    void setUp() {
        List<ColumnMetadata> columns = new ArrayList<>();

        columns.add(new ColumnMetadata("id", DataType.INTEGER, 4, new ArrayList<>(), Collate.NOCASE));
        columns.add(new ColumnMetadata("name", DataType.TEXT, 32, new ArrayList<>(), Collate.NOCASE));

        tableMetadata = new TableMetadata("users", columns);
    }

    @Test
    void testColumnNamesManagement() {
        tableMetadata.addColumnName("email");

        assertEquals(3, tableMetadata.getColumnNames().size());
        assertTrue(tableMetadata.getColumnNames().contains("id"));

        tableMetadata.deleteColumnName("id");

        assertFalse(tableMetadata.getColumnNames().contains("id"));
        assertEquals(2, tableMetadata.getColumnNames().size());
    }

    @Test
    void testCounters() {
        tableMetadata.addAutoIncrement();
        tableMetadata.addDefault();
        tableMetadata.addDefault();

        assertEquals(1, tableMetadata.getCountAutoIncrements());
        assertEquals(2, tableMetadata.getCountDefaults());
    }

    @Test
    void testEmptyConstructorInitialization() {
        TableMetadata metadata = new TableMetadata();
        assertNotNull(metadata.getColumnNames(), "Список имен колонок не должен быть null");
    }
}