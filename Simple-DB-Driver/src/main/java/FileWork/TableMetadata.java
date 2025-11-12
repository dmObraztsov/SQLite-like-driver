package FileWork;

import java.util.List;
import java.util.Map;

public class TableMetadata {
    private String tableName;
    private List<ColumnMetadata> columns; // Описание всех колонок
    private int rowCount;                 // Количество строк
    private String filePath;              // Путь к файлам таблицы
    private Map<String, Object> stats;    // Статистика для оптимизации
}