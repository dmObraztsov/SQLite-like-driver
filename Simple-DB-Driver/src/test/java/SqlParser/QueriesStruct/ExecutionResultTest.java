package SqlParser.QueriesStruct;

import Yadro.DataStruct.Row;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionResultTest {

    @Test
    void shouldInitializeWithEmptyRowsWhenUsingTwoArgConstructor() {
        ExecutionResult result = new ExecutionResult(true, "Success");

        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        assertNotNull(result.getRows());
        assertTrue(result.getRows().isEmpty());
    }

    @Test
    void shouldCopyRowsWhenUsingThreeArgConstructor() {
        Row mockRow = new Row(Map.of("id", "1", "name", "test"));
        List<Row> rows = List.of(mockRow);

        ExecutionResult result = new ExecutionResult(true, "Success", rows);

        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        assertEquals(1, result.getRows().size());
        assertEquals(mockRow, result.getRows().getFirst());
    }

    @Test
    void shouldUpdateStateWhenUsingSetters() {
        ExecutionResult result = new ExecutionResult(false, "Fail");

        result.setSuccess(true);
        result.setMessage("New Message");
        List<Row> newRows = List.of(new Row(Map.of("id", "1", "name", "test")));
        result.setRows(newRows);

        assertTrue(result.isSuccess());
        assertEquals("New Message", result.getMessage());
        assertEquals(newRows, result.getRows());
    }
}