package FileWork;

import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;

import java.io.File;
import java.util.Date;
import java.util.Objects;

public class FileManager {
    private final FileStorage fileStorage;
    private String nameDB;
    private static final String NO_USE_DB = "";

    public FileManager(FileStorage someStorage, String nameDB) {
        if (someStorage == null) {
            throw new IllegalArgumentException("FileStorage cannot be null");
        }
        this.fileStorage = someStorage;
        setNameDB(nameDB);
    }

    public FileManager(FileStorage someStorage)
    {
        this(someStorage, NO_USE_DB);
    }

    public boolean createDB(String name) {
        Date current = new Date();
        DatabaseMetadata databaseMetadata = new DatabaseMetadata(name, "1.0.0", "UTF-16", "Default", current, current);
        return fileStorage.createDirectory(PathManager.getDatabasePath(name)) &
                fileStorage.writeFile(PathManager.getDatabaseMetadataPath(name), databaseMetadata);
    }

    public boolean dropDB(String name) {
        if (Objects.equals(nameDB, name)) {
            nameDB = NO_USE_DB;
        }

        return fileStorage.deleteDirectory(PathManager.getDatabasePath(name));
    }

    public boolean useDB(String nameDB) {
        return setNameDB(nameDB);
    }

    public DatabaseMetadata getDatabaseMetadata() {
        return fileStorage.readFile(PathManager.getDatabasePath(nameDB), DatabaseMetadata.class);
    }

    public boolean createTable(String tableName) {
        TableMetadata tableMetadata = new TableMetadata(tableName, 0);
        return fileStorage.createDirectory(PathManager.getTablePath(nameDB, tableName)) &
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }

    public boolean dropTable(String tableName) {
        return fileStorage.deleteDirectory(PathManager.getTablePath(nameDB, tableName));
    }

    public boolean renameTable(String tableName, String changeTableName) {
        TableMetadata toChange = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        toChange.setName(changeTableName);

        return fileStorage.renameDirectory(PathManager.getTablePath(nameDB, tableName), PathManager.getTablePath(nameDB, changeTableName)) &
                fileStorage.deleteFile(PathManager.getTableMetadataPath(nameDB, changeTableName)) &
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, changeTableName), toChange);
    }

    public TableMetadata getTableMetadata(String tableName) {
        return fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
    }

    public Column loadColumn(String tableName, String columnName) {
        return fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName), Column.class);
    }

    public boolean saveColumn(String tableName, Column column) {
        if (column == null) {
            System.out.println("Column cannot be null");
            return false;
        }

        if(!new File(PathManager.getTableDataPath(nameDB, tableName)).exists())
        {
            fileStorage.createDirectory(PathManager.getTableDataPath(nameDB, tableName));
        }

        ColumnMetadata columnMetadata = new ColumnMetadata(column);
        TableMetadata tableMetadata = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        tableMetadata.setColumnCount(tableMetadata.getColumnCount() + 1);
        tableMetadata.addColumnName(column.getName());

        return fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, column.getName()), column) &
                fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, column.getName()), columnMetadata) &
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }

    public boolean deleteColumn(String tableName, String columnName) {
        TableMetadata tableMetadata = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        tableMetadata.setColumnCount(tableMetadata.getColumnCount() - 1);
        tableMetadata.deleteColumnName(columnName);

        return fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName)) &
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName)) &
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }

    public boolean renameColumn(String tableName, String columnName, String changeColumnName) {
        Column toChange = fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName), Column.class);
        toChange.setName(changeColumnName);
        ColumnMetadata toChangeMeta = fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName),
                ColumnMetadata.class);
        return fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName)) &
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName)) &
                fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, changeColumnName), toChange) &
                fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, changeColumnName), toChangeMeta);
    }

    public String getNameDB() {
        return nameDB;
    }

    public boolean setNameDB(String nameDB) {
        if (nameDB == null || nameDB.trim().isEmpty()) {
            this.nameDB = NO_USE_DB;
            return true;
        }

        if (fileStorage.exists(PathManager.getDatabasePath(nameDB))) {
            this.nameDB = nameDB;
            return true;
        }

        System.out.println("Database does not exist: " + nameDB);
        return false;
    }
}