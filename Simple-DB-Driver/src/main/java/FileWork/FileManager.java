package FileWork;

import Yadro.DataStruct.Column;

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

    public boolean createDB(String name)
    {
        return fileStorage.createDirectory(name);
    }

    public boolean dropDB(String name) {
        if (Objects.equals(nameDB, name)) {
            nameDB = NO_USE_DB;
        }

        return fileStorage.deleteDirectory(name);
    }

    public boolean useDB(String nameDB) {
        return setNameDB(nameDB);
    }

    public boolean createTable(String tableName)
    {
        return fileStorage.createDirectory(nameDB + '/' + tableName);
    }

    public boolean dropTable(String tableName) {
        String path = nameDB + '/' + tableName;
        return fileStorage.deleteDirectory(path);
    }

    public boolean renameTable(String tableName, String changeTableName)
    {
        String path = nameDB + '/' + tableName;
        return fileStorage.renameDirectory(path, nameDB + '/' + changeTableName);
    }

    public Column loadColumn(String tableName, String columnName) {
        String path = nameDB + '/' + tableName + '/' + columnName;
        return fileStorage.readFile(path);
    }

    public boolean saveColumn(String tableName, Column column) {
        if (column == null) {
            System.out.println("Column cannot be null");
            return false;
        }

        String path = nameDB + '/' + tableName + '/' + column.getName();
        return fileStorage.writeFile(path, column);
    }

    public boolean deleteColumn(String tableName, String columnName) {
        String path = nameDB + '/' + tableName + '/' + columnName;
        return fileStorage.deleteFile(path);
    }

    public boolean renameColumn(String tableName, String columnName, String changeColumnName)
    {
        String path = nameDB + '/' + tableName + '/' + columnName;
        String newPath = nameDB + '/' + tableName + '/' + changeColumnName;
        Column toChange = fileStorage.readFile(path);
        toChange.setName(changeColumnName);
        return fileStorage.writeFile(newPath, toChange) & fileStorage.deleteFile(path);
    }

    public String getNameDB() {
        return nameDB;
    }

    public boolean setNameDB(String nameDB) {
        if (nameDB == null || nameDB.trim().isEmpty()) {
            this.nameDB = NO_USE_DB;
            return true;
        }

        if (fileStorage.exists(nameDB)) {
            this.nameDB = nameDB;
            return true;
        }

        System.out.println("Database does not exist: " + nameDB);
        return false;
    }
}