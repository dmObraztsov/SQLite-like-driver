package FileWork;

import Exceptions.FileManagerException;
import Exceptions.FileStorageException;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;

public interface FileStorage {
    boolean exists(String path);
    <T> T readFile(String path, Class<T> type) throws FileStorageException;
    <T> void writeFile(String path, T content) throws FileStorageException;
    void deleteFile(String path) throws FileStorageException;
    void renameFile(String path, String newName) throws FileStorageException;
    void createDirectory(String path) throws FileStorageException;
    void deleteDirectory(String path) throws FileStorageException;
    void renameDirectory(String path, String newName) throws FileStorageException;
}