package FileWork.Metadata;

import Yadro.DataStruct.Collate;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColumnMetadataTest {

    private ColumnMetadata column;
    private ArrayList<Constraints> constraints;

    @BeforeEach
    void setUp() {
        constraints = new ArrayList<>(Arrays.asList(
                Constraints.NOT_NULL,
                Constraints.UNIQUE
        ));

        column = new ColumnMetadata(
                "email",
                DataType.TEXT,
                255,
                constraints,
                Collate.NOCASE,
                5,
                "a@example.com",
                "z@example.com"
        );
    }

    @Test
    @DisplayName("Should create column with all properties")
    void shouldCreateColumnWithAllProperties() {
        // Then
        assertEquals("email", column.getName());
        assertEquals(DataType.TEXT, column.getType());
        assertEquals(255, column.getSize());
        assertEquals(2, column.getConstraints().size());
        assertEquals(Collate.NOCASE, column.getCollate());
        assertEquals(5, column.getNullCount());
        assertEquals("a@example.com", column.getMinValue());
        assertEquals("z@example.com", column.getMaxValue());
    }

    @Test
    @DisplayName("Should get correct constraints")
    void shouldGetCorrectConstraints() {
        // When
        List<Constraints> actualConstraints = column.getConstraints();

        // Then
        assertTrue(actualConstraints.contains(Constraints.NOT_NULL));
        assertTrue(actualConstraints.contains(Constraints.UNIQUE));
        assertEquals(2, actualConstraints.size());
    }

    @Test
    @DisplayName("Should set and get name")
    void shouldSetAndGetName() {
        // When
        column.setName("new_email");

        // Then
        assertEquals("new_email", column.getName());
    }

    @Test
    @DisplayName("Should set and get type")
    void shouldSetAndGetType() {
        // When
        column.setType(DataType.INTEGER);

        // Then
        assertEquals(DataType.INTEGER, column.getType());
    }

    @Test
    @DisplayName("Should set and get size")
    void shouldSetAndGetSize() {
        // When
        column.setSize(500);

        // Then
        assertEquals(500, column.getSize());
    }

    @Test
    @DisplayName("Should set and get constraints")
    void shouldSetAndGetConstraints() {
        // Given
        ArrayList<Constraints> newConstraints = new ArrayList<>(Arrays.asList(
                Constraints.PRIMARY_KEY,
                Constraints.AUTOINCREMENT
        ));

        // When
        column.setConstraints(newConstraints);

        // Then
        List<Constraints> actual = column.getConstraints();
        assertEquals(2, actual.size());
        assertTrue(actual.contains(Constraints.PRIMARY_KEY));
        assertTrue(actual.contains(Constraints.AUTOINCREMENT));
        assertFalse(actual.contains(Constraints.NOT_NULL)); // Старые удалены
    }

    @Test
    @DisplayName("Should set and get collate")
    void shouldSetAndGetCollate() {
        // When
        column.setCollate(Collate.BINARY);

        // Then
        assertEquals(Collate.BINARY, column.getCollate());
    }

    @Test
    @DisplayName("Should set and get null count")
    void shouldSetAndGetNullCount() {
        // When
        column.setNullCount(10);

        // Then
        assertEquals(10, column.getNullCount());
    }

    @Test
    @DisplayName("Should set and get min value")
    void shouldSetAndGetMinValue() {
        // When
        column.setMinValue(1);

        // Then
        assertEquals(1, column.getMinValue());
    }

    @Test
    @DisplayName("Should set and get max value")
    void shouldSetAndGetMaxValue() {
        // When
        column.setMaxValue(100);

        // Then
        assertEquals(100, column.getMaxValue());
    }

    @Test
    @DisplayName("Should create column with empty constraints")
    void shouldCreateColumnWithEmptyConstraints() {
        // Given
        ColumnMetadata emptyColumn = new ColumnMetadata(
                "id",
                DataType.INTEGER,
                0,
                new ArrayList<>(),
                null,
                0,
                null,
                null
        );

        // Then
        assertTrue(emptyColumn.getConstraints().isEmpty());
        assertNull(emptyColumn.getCollate());
        assertNull(emptyColumn.getMinValue());
        assertNull(emptyColumn.getMaxValue());
    }

    @Test
    @DisplayName("Should handle null values in constructor")
    void shouldHandleNullValuesInConstructor() {
        // Given
        ColumnMetadata nullColumn = new ColumnMetadata(
                "test",
                null,
                0,
                null,
                null,
                0,
                null,
                null
        );

        // Then
        assertEquals("test", nullColumn.getName());
        assertNull(nullColumn.getType());
        assertNull(nullColumn.getConstraints());
        assertNull(nullColumn.getCollate());
        assertNull(nullColumn.getMinValue());
        assertNull(nullColumn.getMaxValue());
    }

    @Test
    @DisplayName("Should add constraint to existing list")
    void shouldAddConstraintToList() {
        // Given
        ArrayList<Constraints> constraintsList = column.getConstraints();

        // When
        constraintsList.add(Constraints.PRIMARY_KEY);

        // Then
        assertEquals(3, column.getConstraints().size());
        assertTrue(column.getConstraints().contains(Constraints.PRIMARY_KEY));
    }

    @Test
    @DisplayName("Should update all properties")
    void shouldUpdateAllProperties() {
        // When
        column.setName("username");
        column.setType(DataType.TEXT);
        column.setSize(100);
        column.setCollate(Collate.BINARY);
        column.setNullCount(3);
        column.setMinValue("admin");
        column.setMaxValue("user123");

        // Then
        assertEquals("username", column.getName());
        assertEquals(DataType.TEXT, column.getType());
        assertEquals(100, column.getSize());
        assertEquals(Collate.BINARY, column.getCollate());
        assertEquals(3, column.getNullCount());
        assertEquals("admin", column.getMinValue());
        assertEquals("user123", column.getMaxValue());
    }
}