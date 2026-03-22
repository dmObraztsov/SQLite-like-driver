package Yadro.DataStruct;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RowColumnTest {

    @Test
    void columnAddDataShouldInsertByIndex() {
        Column column = new Column();
        column.addData(0, "a");
        column.addData(1, "b");

        assertThat(column.getData()).containsExactly("a", "b");
    }

    @Test
    void rowShouldReturnSelectedValuesMap() {
        Row row = new Row(Map.of("id", "1", "name", "Ann", "city", "Paris"));

        Map<String, String> selected = row.getValuesMap(List.of("name", "city"));

        assertThat(selected).containsExactlyInAnyOrderEntriesOf(Map.of("name", "Ann", "city", "Paris"));
    }
}
