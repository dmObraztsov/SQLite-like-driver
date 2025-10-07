package FileWork;

public class ColumnFileManager {
    private final FileStorage fileStorage;

    public Column loadColumn(String tableName, String columnName, DataType type) { ... }
    public void saveColumn(String tableName, Column column) { ... }
}

/**
 * Класс для работы с колонками как с файлами
 */