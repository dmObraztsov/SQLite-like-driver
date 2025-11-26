package FileWork;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PathManagerTest {

    private static final String BASE_PATH = "src/main/data/";
    private static final String DB_NAME = "testdb";
    private static final String TABLE_NAME = "testtable";
    private static final String COLUMN_NAME = "testcolumn";

    @Test
    @DisplayName("Should return correct database path")
    void testGetDatabasePath() {
        // When
        String result = PathManager.getDatabasePath(DB_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME, result);
    }

    @Test
    @DisplayName("Should return correct database metadata path")
    void testGetDatabaseMetadataPath() {
        // When
        String result = PathManager.getDatabaseMetadataPath(DB_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME + "/metadata.json", result);
    }

    @Test
    @DisplayName("Should return correct table path")
    void testGetTablePath() {
        // When
        String result = PathManager.getTablePath(DB_NAME, TABLE_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME + "/tables/" + TABLE_NAME, result);
    }

    @Test
    @DisplayName("Should return correct table metadata path")
    void testGetTableMetadataPath() {
        // When
        String result = PathManager.getTableMetadataPath(DB_NAME, TABLE_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME + "/tables/" + TABLE_NAME + "/metadata.json", result);
    }

    @Test
    @DisplayName("Should return correct table data path")
    void testGetTableDataPath() {
        // When
        String result = PathManager.getTableDataPath(DB_NAME, TABLE_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME + "/tables/" + TABLE_NAME + "/data/", result);
    }

    @Test
    @DisplayName("Should return correct column path")
    void testGetColumnPath() {
        // When
        String result = PathManager.getColumnPath(DB_NAME, TABLE_NAME, COLUMN_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME + "/tables/" + TABLE_NAME + "/data/" + COLUMN_NAME + ".json", result);
    }

    @Test
    @DisplayName("Should return correct column metadata path")
    void testGetColumnMetadataPath() {
        // When
        String result = PathManager.getColumnMetadataPath(DB_NAME, TABLE_NAME, COLUMN_NAME);

        // Then
        assertEquals(BASE_PATH + DB_NAME + "/tables/" + TABLE_NAME + "/data/" + COLUMN_NAME + ".metadata.json", result);
    }
}