package FileWork;

import Exceptions.EmptyFileException;
import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import lombok.Getter;

import java.util.Date;

public class FileManager {
    private final FileStorage fileStorage;
    @Getter
    private String nameDB;
    private static final String NO_USE_DB = "";

    public FileManager(FileStorage someStorage) {
        if (someStorage == null) {
            throw new IllegalArgumentException("FileStorage cannot be null");
        }
        this.fileStorage = someStorage;
        this.nameDB = NO_USE_DB;
    }

    public void createDB(String name) throws FileStorageException {
        Date current = new Date();
        DatabaseMetadata metadata = new DatabaseMetadata(name, "1.0.0", "UTF-16", "Default", current, current);

        fileStorage.createDirectory(PathManager.getDatabasePath(name));
        fileStorage.writeFile(PathManager.getDatabaseMetadataPath(name), metadata);
    }

    public void dropDB(String name) throws FileStorageException {
        if (name.equals(nameDB)) {
            nameDB = NO_USE_DB;
        }
        fileStorage.deleteDirectory(PathManager.getDatabasePath(name));
    }

    public void useDB(String name) throws FileStorageException {
        if (name == null || name.trim().isEmpty()) {
            this.nameDB = NO_USE_DB;
            return;
        }

        if (!fileStorage.exists(PathManager.getDatabasePath(name))) {
            throw new EmptyFileException("Database not found: " + name);
        }

        this.nameDB = name;
    }

    public void saveTableMetadata(String tableName, TableMetadata metadata) throws FileStorageException {
        fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), metadata);
    }

    public TableMetadata loadTableMetadata(String tableName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
    }

    public void createTableStructure(String tableName) throws FileStorageException {
        fileStorage.createDirectory(PathManager.getTablePath(nameDB, tableName));
        fileStorage.createDirectory(PathManager.getTableDataPath(nameDB, tableName));
    }

    public void dropTableStructure(String tableName) throws FileStorageException {
        fileStorage.deleteDirectory(PathManager.getTablePath(nameDB, tableName));
    }

    public Column loadColumnData(String tableName, String columnName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName), Column.class);
    }

    public void saveColumnData(String tableName, String columnName, Column column) throws FileStorageException {
        fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnName), column);
    }

    public ColumnMetadata loadColumnMetadata(String tableName, String columnName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
    }

    public void saveColumnMetadata(String tableName, String columnName, ColumnMetadata metadata) throws FileStorageException {
        fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), metadata);
    }

    public void deleteColumnFiles(String tableName, String columnName) throws FileStorageException {
        fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName));
        fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName));
    }

    public void renameColumnFiles(String tableName, String oldName, String newName) throws FileStorageException {
        fileStorage.renameFile(PathManager.getColumnPath(nameDB, tableName, oldName), PathManager.getColumnPath(nameDB, tableName, newName));
        fileStorage.renameFile(PathManager.getColumnMetadataPath(nameDB, tableName, oldName), PathManager.getColumnMetadataPath(nameDB, tableName, newName));
    }

    public boolean tableExists(String tableName) {
        return fileStorage.exists(PathManager.getTablePath(nameDB, tableName));
    }

    public void renameFile(String oldPath, String newPath) throws FileStorageException {
        fileStorage.renameFile(oldPath, newPath);
    }

    public void renameDirectory(String oldName, String newName) throws FileStorageException {
        fileStorage.renameDirectory(PathManager.getTablePath(this.getNameDB(), oldName), PathManager.getTablePath(this.getNameDB(), newName));
    }
}