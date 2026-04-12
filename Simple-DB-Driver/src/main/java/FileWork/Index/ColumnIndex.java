package FileWork.Index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnIndex {

    private String columnName;

    private Map<String, List<Integer>> index = new HashMap<>();

    public void addEntry(String value, int rowIndex) {
        if (value == null || value.equals("NULL")) return;
        index.computeIfAbsent(value, k -> new ArrayList<>()).add(rowIndex);
    }

    public void removeEntry(String value, int rowIndex) {
        if (value == null || value.equals("NULL")) return;
        List<Integer> rows = index.get(value);
        if (rows != null) {
            rows.remove(Integer.valueOf(rowIndex));
            if (rows.isEmpty()) {
                index.remove(value);
            }
        }
    }

    public void shiftAfterDelete(int deletedRow) {
        Map<String, List<Integer>> rebuilt = new HashMap<>();
        for (Map.Entry<String, List<Integer>> e : index.entrySet()) {
            List<Integer> newRows = new ArrayList<>();
            for (int r : e.getValue()) {
                if (r < deletedRow) {
                    newRows.add(r);
                }
                else if (r > deletedRow) {
                    newRows.add(r - 1);
                }
                // r == deletedRow уже удалён через removeEntry
            }
            if (!newRows.isEmpty()) {
                rebuilt.put(e.getKey(), newRows);
            }
        }
        this.index = rebuilt;
    }

    public List<Integer> lookup(String value) {
        if (value == null || value.equals("NULL")) {
            return List.of();
        }
        return index.getOrDefault(value, List.of());
    }
}
