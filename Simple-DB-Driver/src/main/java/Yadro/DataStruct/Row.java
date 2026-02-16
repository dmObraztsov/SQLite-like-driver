package Yadro.DataStruct;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record Row(Map<String, String> values) {

    public String get(String columnName) {
        return values.get(columnName);
    }

    public Map<String, String> getValuesMap() {
        return values;
    }

    public Map<String, String> getValuesMap(List<String> keys) {
        return values.entrySet().stream()
                .filter(entry -> keys.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
