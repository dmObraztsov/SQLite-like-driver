package FileWork.Metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableMetadataTest {

    private TableMetadata tableMetadata;

    @BeforeEach
    void setUp() {
        tableMetadata = new TableMetadata("users", 2, 0, 0);
    }

    @Test
    void testColumnNamesManagement() {
        tableMetadata.addColumnName("id");
        tableMetadata.addColumnName("email");

        assertEquals(2, tableMetadata.getColumnNames().size());
        assertTrue(tableMetadata.getColumnNames().contains("id"));

        tableMetadata.deleteColumnName("id");

        assertFalse(tableMetadata.getColumnNames().contains("id"));
        assertEquals(1, tableMetadata.getColumnNames().size());
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