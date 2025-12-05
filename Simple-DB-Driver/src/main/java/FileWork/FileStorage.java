package FileWork;

import Exceptions.FileStorageException;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;

public interface FileStorage {
    boolean exists(String path);
    <T> T readFile(String path, Class<T> type);
    <T> void writeFile(String path, T content) throws FileStorageException;
    boolean deleteFile(String path);
    boolean renameFile(String path, String newName);
    boolean createDirectory(String path);
    boolean deleteDirectory(String path);
    boolean renameDirectory(String path, String newName);
}