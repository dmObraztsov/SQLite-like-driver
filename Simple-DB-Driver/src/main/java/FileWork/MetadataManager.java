package FileWork;

public class MetadataManager {
    private final FileStorage fileStorage;

    public TableMetadata loadTableMetadata(String tableName) { ... }
    public void saveTableMetadata(String tableName, TableMetadata metadata) { ... }
}

/**
 * Класс для работы с метаданными на уровне таблицы
 */