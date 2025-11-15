package FileWork;

import Yadro.DataStruct.Column;

public interface FileStorage {
    boolean exists(String path);
    Column readFile(String path);
    boolean writeFile(String path, Column content);
    boolean deleteFile(String path);
    boolean renameFile(String path, String newName);
    boolean createDirectory(String path);
    boolean deleteDirectory(String path);
    boolean renameDirectory(String path, String newName);
}