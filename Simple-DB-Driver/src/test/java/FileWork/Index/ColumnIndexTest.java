package FileWork.Index;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ColumnIndexTest {

    private ColumnIndex columnIndex;

    @BeforeEach
    void setUp() {
        columnIndex = new ColumnIndex();
        columnIndex.setColumnName("age");
    }

    @Test
    void testAddAndLookup() {
        columnIndex.addEntry("25", 0);
        columnIndex.addEntry("30", 1);
        columnIndex.addEntry("25", 2);

        List<Integer> result25 = columnIndex.lookup("25");
        List<Integer> result30 = columnIndex.lookup("30");

        assertEquals(2, result25.size());
        assertTrue(result25.contains(0));
        assertTrue(result25.contains(2));

        assertEquals(1, result30.size());
        assertEquals(1, result30.get(0));
    }

    @Test
    void testRemoveEntry() {
        columnIndex.addEntry("apple", 10);
        columnIndex.addEntry("apple", 11);

        columnIndex.removeEntry("apple", 10);

        List<Integer> result = columnIndex.lookup("apple");
        assertEquals(1, result.size());
        assertFalse(result.contains(10));
        assertTrue(result.contains(11));

        // Удаляем последнюю запись
        columnIndex.removeEntry("apple", 11);
        assertTrue(columnIndex.getIndex().isEmpty());
    }

    @Test
    void testShiftAfterDelete() {
        columnIndex.addEntry("A", 0);
        columnIndex.addEntry("B", 1);
        columnIndex.addEntry("A", 2);
        columnIndex.addEntry("C", 3);

        columnIndex.removeEntry("B", 1);
        columnIndex.shiftAfterDelete(1);

        List<Integer> rowsA = columnIndex.lookup("A");
        assertEquals(2, rowsA.size());
        assertTrue(rowsA.contains(0));
        assertTrue(rowsA.contains(1));

        List<Integer> rowsC = columnIndex.lookup("C");
        assertEquals(2, rowsC.get(0));
    }

    @Test
    void testNullBehavior() {
        columnIndex.addEntry(null, 1);
        columnIndex.addEntry("NULL", 2);

        assertTrue(columnIndex.getIndex().isEmpty());

        assertEquals(0, columnIndex.lookup(null).size());
        assertEquals(0, columnIndex.lookup("NULL").size());
    }

    @Test
    void testLookupNonExistent() {
        List<Integer> result = columnIndex.lookup("missing");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}