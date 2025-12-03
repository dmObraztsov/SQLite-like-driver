package FileWork;

import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;

public interface FileStorage {
    boolean exists(String path);
    <T> T readFile(String path, Class<T> type);
    <T> boolean writeFile(String path, T content);
    boolean deleteFile(String path);
    boolean renameFile(String path, String newName);
    boolean createDirectory(String path);
    boolean deleteDirectory(String path);
    boolean renameDirectory(String path, String newName);
}