package FileWork.Binary;

import FileWork.Metadata.ColumnMetadata;
import Yadro.DataStruct.Row;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class RandomAccessBinaryStorage {

    public void writeRow(String path, int rowIndex, Row row, List<ColumnMetadata> columns) throws IOException {
        int recordSize = calculateRecordSize(columns);
        byte[] rowData = BinaryRowMapper.rowToBytes(row, columns);

        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            // Прыгаем сразу на нужную позицию
            raf.seek((long) rowIndex * recordSize);
            raf.write(rowData);
        }
    }

    public Row readRow(String path, int rowIndex, List<ColumnMetadata> columns) throws IOException {
        int recordSize = calculateRecordSize(columns);
        byte[] buffer = new byte[recordSize];

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            raf.seek((long) rowIndex * recordSize);
            raf.readFully(buffer);
        }

        return BinaryRowMapper.bytesToRow(buffer, columns);
    }

    public List<Row> readAllRows(String path, List<ColumnMetadata> columns) throws IOException {
        List<Row> rows = new ArrayList<>();
        int recordSize = calculateRecordSize(columns);

        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long fileLength = raf.length();
            int rowSize = columns.stream().mapToInt(ColumnMetadata::getSize).sum();
            if (rowSize <= 0) {
                throw new IOException("Invalid table schema: row size is 0. Check column metadata.");
            }
            int rowCount = (int) (fileLength / recordSize);

            for (int i = 0; i < rowCount; i++) {
                byte[] buffer = new byte[recordSize];
                raf.readFully(buffer);
                rows.add(BinaryRowMapper.bytesToRow(buffer, columns));
            }
        }
        return rows;
    }

    private int calculateRecordSize(List<ColumnMetadata> columns) {
        return columns.stream().mapToInt(ColumnMetadata::getSize).sum();
    }
}