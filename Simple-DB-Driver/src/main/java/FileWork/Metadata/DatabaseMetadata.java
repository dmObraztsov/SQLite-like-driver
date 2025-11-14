package FileWork.Metadata;

import java.util.Date;

public class DatabaseMetadata {
    private String name;                    // Имя БД
    private String version;                 // Версия схемы БД
    private String encoding;               // Кодировка (UTF-8, etc)
    private String collation;              // Правила сортировки по умолчанию
    private Date createdDate;              // Дата создания
    private Date lastModified;             // Дата последнего изменения
    private long totalSize;                // Общий размер на диске
}
