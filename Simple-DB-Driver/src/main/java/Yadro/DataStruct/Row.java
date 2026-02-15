package Yadro.DataStruct;

import java.util.Map;

public record Row(Map<String, String> values) {

    public String get(String columnName) {
        return values.get(columnName);
    }

    public Map<String, String> getValuesMap() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
