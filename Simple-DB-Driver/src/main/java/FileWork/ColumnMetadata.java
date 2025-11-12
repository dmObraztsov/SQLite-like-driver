package FileWork;

import Yadro.DataStruct.DataType;

public class ColumnMetadata {
    private String name;           // Имя колонки
    private DataType type;         // Тип данных (INT, VARCHAR, etc)
    private String filePath;       // Путь к файлу с данными колонки
    private int nullCount;         // Количество NULL значений
    private Object minValue;       // Минимальное значение (для оптимизации)
    private Object maxValue;       // Максимальное значение
    private long dataSize;         // Размер данных в байтах
}