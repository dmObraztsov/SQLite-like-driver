package FileWork;

public interface FileStorage {
    boolean exists(String path);
    String readFile(String path);
    void writeFile(String path, String content);
    void createDirectory(String path);
    void deleteDirectory(String path);
}