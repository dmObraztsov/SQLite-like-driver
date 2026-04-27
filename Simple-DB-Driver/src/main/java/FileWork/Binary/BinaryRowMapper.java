package FileWork.Binary;

import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.Row;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryRowMapper {

    public static byte[] rowToBytes(Row row, List<ColumnMetadata> columns) {
        int totalSize = columns.stream().mapToInt(ColumnMetadata::getSize).sum();
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        for (ColumnMetadata col : columns) {
            String value = row.get(col.getName());
            if (value == null) {
                value = (col.getDefaultValue() != null) ? col.getDefaultValue() : "NULL";
            }

            byte[] rawBytes = value.getBytes(StandardCharsets.UTF_8);
            int colSize = col.getSize();

            if (rawBytes.length >= colSize) {
                // Если данные не влезают, обрезаем (важно для фиксированного формата)
                buffer.put(rawBytes, 0, colSize);
            } else {
                // Пишем данные и добиваем остаток нулями (padding)
                buffer.put(rawBytes);
                buffer.put(new byte[colSize - rawBytes.length]);
            }
        }
        return buffer.array();
    }

    public static Row bytesToRow(byte[] bytes, List<ColumnMetadata> columns) {
        Map<String, String> values = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        for (ColumnMetadata col : columns) {
            byte[] colBytes = new byte[col.getSize()];
            buffer.get(colBytes);

            // trim() важен, чтобы убрать нулевые байты заполнителя
            String val = new String(colBytes, StandardCharsets.UTF_8).trim();
            values.put(col.getName(), val);
        }
        return new Row(values);
    }
}