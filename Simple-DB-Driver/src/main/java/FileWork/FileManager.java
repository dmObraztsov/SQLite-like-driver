package FileWork;

import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;

import java.io.File;
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
        DatabaseMetadata databaseMetadata = new DatabaseMetadata();
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

    public boolean createTable(String tableName) {
        TableMetadata tableMetadata = new TableMetadata();
        return fileStorage.createDirectory(PathManager.getTablePath(nameDB, tableName)) &
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }

    public boolean dropTable(String tableName) {
        return fileStorage.deleteDirectory(PathManager.getTablePath(nameDB, tableName));
    }

    public boolean renameTable(String tableName, String changeTableName) {
        //TODO need to change name in metadata of table
        return fileStorage.renameDirectory(PathManager.getTablePath(nameDB, tableName), changeTableName);
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

        ColumnMetadata columnMetadata = new ColumnMetadata();
        return fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, column.getName()), column) &&
                fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, column.getName()), columnMetadata);
    }

    public boolean deleteColumn(String tableName, String columnName) {
        return fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName)) &&
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName));
    }

    public boolean renameColumn(String tableName, String columnName, String changeColumnName) {
        Column toChange = fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName), Column.class);
        toChange.setName(changeColumnName);
        //TODO also rename in column metadata
        return fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, changeColumnName), toChange)
                & fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName));
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