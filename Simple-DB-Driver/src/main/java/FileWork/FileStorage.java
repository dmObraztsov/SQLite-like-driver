package FileWork;

public interface FileStorage {
    boolean exists(String path);
    String readFile(String path);
    void writeFile(String path, String content);
    void createDirectory(String path);
}

public class JsonFileStorage implements FileStorage { ... }

/**
 * Здесь идея уйти от конкретно JSON-файлов и перейти к абстракции каких либо файлов, чтобы потом когда
 * будем писать для бинарника достаточно было бы чисто олин класс переписать, идея по-моему здравая
 */