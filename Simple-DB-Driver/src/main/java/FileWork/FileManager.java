package FileWork;

import Exceptions.EmptyFileException;
import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import FileWork.WAL.WalEntry;
import Yadro.DataStruct.Column;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        fileStorage.createDirectory(PathManager.getWalDir(name));
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
        fileStorage.renameFile(
                PathManager.getColumnPath(nameDB, tableName, oldName),
                PathManager.getColumnPath(nameDB, tableName, newName));
        fileStorage.renameFile(
                PathManager.getColumnMetadataPath(nameDB, tableName, oldName),
                PathManager.getColumnMetadataPath(nameDB, tableName, newName));
    }

    public boolean tableExists(String tableName) {
        return fileStorage.exists(PathManager.getTablePath(nameDB, tableName));
    }

    public void renameFile(String oldPath, String newPath) throws FileStorageException {
        fileStorage.renameFile(oldPath, newPath);
    }

    public void renameDirectory(String oldName, String newName) throws FileStorageException {
        fileStorage.renameDirectory(
                PathManager.getTablePath(this.getNameDB(), oldName),
                PathManager.getTablePath(this.getNameDB(), newName));
    }

    public void ensureWalDirExists() throws FileStorageException {
        String walDir = PathManager.getWalDir(nameDB);
        if (!fileStorage.exists(walDir)) {
            fileStorage.createDirectory(walDir);
        }
    }

    public void writeWalAtomic(String txId, WalEntry entry) throws FileStorageException {
        String tmpPath   = PathManager.getWalTmpPath(nameDB, txId);
        String finalPath = PathManager.getWalPath(nameDB, txId);

        fileStorage.writeFile(tmpPath, entry);
        fileStorage.renameFile(tmpPath, finalPath);
    }

    public WalEntry loadWal(String txId) throws FileStorageException {
        return fileStorage.readFile(PathManager.getWalPath(nameDB, txId), WalEntry.class);
    }

    public void deleteWal(String txId) throws FileStorageException {
        String path = PathManager.getWalPath(nameDB, txId);
        if (fileStorage.exists(path)) {
            fileStorage.deleteFile(path);
        }
    }

    public void deleteWalTmp(String txId) throws FileStorageException {
        String path = PathManager.getWalTmpPath(nameDB, txId);
        if (fileStorage.exists(path)) {
            fileStorage.deleteFile(path);
        }
    }

    public List<String> listPendingWalIds() {
        String walDir = PathManager.getWalDir(nameDB);
        File dir = new File(walDir);
        List<String> ids = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) return ids;

        File[] files = dir.listFiles();
        if (files == null) return ids;

        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".wal")) {
                ids.add(name.substring(0, name.length() - 4));
            }
        }
        return ids;
    }

    public List<String> listWalTmpIds() {
        String walDir = PathManager.getWalDir(nameDB);
        File dir = new File(walDir);
        List<String> ids = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) return ids;

        File[] files = dir.listFiles();
        if (files == null) return ids;

        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".wal.tmp")) {
                ids.add(name.substring(0, name.length() - 8));
            }
        }
        return ids;
    }
}
