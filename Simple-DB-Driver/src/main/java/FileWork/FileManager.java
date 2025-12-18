package FileWork;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import Yadro.DataStruct.PrimaryKeyMap;

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
        try {
            Date current = new Date();
            DatabaseMetadata databaseMetadata = new DatabaseMetadata(name, "1.0.0", "UTF-16", "Default", current, current);

            if (!fileStorage.createDirectory(PathManager.getDatabasePath(name))) {
                return false;
            }

            fileStorage.writeFile(PathManager.getDatabaseMetadataPath(name), databaseMetadata);

            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace(); //TODO: здесь лучше использовать логгер
            return false;
        }
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

    public DatabaseMetadata loadDatabaseMetadata() throws FileStorageException {
        return fileStorage.readFile(PathManager.getDatabasePath(nameDB), DatabaseMetadata.class);
    }

    public boolean createTable(String tableName) {
        TableMetadata tableMetadata = new TableMetadata(tableName, 0, 0, 0);

        try {
            if (!fileStorage.createDirectory(PathManager.getTablePath(nameDB, tableName))) {
                return false;
            }

            fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);

            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean dropTable(String tableName) {
        return fileStorage.deleteDirectory(PathManager.getTablePath(nameDB, tableName));
    }


    public boolean renameTable(String tableName, String changeTableName) throws FileStorageException {
        TableMetadata toChange = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        toChange.setName(changeTableName);

        try {
            if (!fileStorage.renameDirectory(PathManager.getTablePath(nameDB, tableName), PathManager.getTablePath(nameDB, changeTableName))) {
                return false;
            }

            if (!fileStorage.deleteFile(PathManager.getTableMetadataPath(nameDB, changeTableName))) {
                return false;
            }

            fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, changeTableName), toChange);

            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

    }

    public TableMetadata loadTableMetadata(String tableName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
    }

    public Column loadColumn(String tableName, String columnName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName), Column.class);
    }

    public boolean createColumn(String tableName, ColumnMetadata columnMetadata) throws FileStorageException {
        if (columnMetadata == null) {
            System.out.println("Column cannot be null");
            return false;
        }

        TableMetadata tableMetadata = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        if (tableMetadata == null) {
            System.out.println("Table metadata not found for table: " + tableName);
            return false;
        }

        if (!validateColumn(columnMetadata)) {
            return false;
        }

        updateTableMetadata(tableMetadata, columnMetadata);
        Column column = new Column();
        return writeColumnFiles(tableName, columnMetadata, column, tableMetadata);
    }

    public boolean saveColumn(String tableName, String columnName, Column column){
        try {
            fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnName), column);
            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public ColumnMetadata loadColumnMetadata(String tableName, String columnName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
    }

    public boolean saveColumnMetadata(String tableName, String columnName, ColumnMetadata columnMetadata)
    {
        try {
            fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), columnMetadata);
            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    //этот метод обновлю, когда допишу другие классы исключения и изменю соответствующие методы
    public boolean deleteColumn(String tableName, String columnName) throws FileStorageException {
        TableMetadata tableMetadata = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        tableMetadata.setColumnCount(tableMetadata.getColumnCount() - 1);
        tableMetadata.deleteColumnName(columnName);

        try {
            fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

        return fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName)) &&
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName));
    }

    public boolean renameColumn(String tableName, String columnName, String changeColumnName) throws FileStorageException {
        ColumnMetadata toChangeMeta = fileStorage.readFile(PathManager.
                getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
        toChangeMeta.setName(changeColumnName);

        try {
            fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, changeColumnName), toChangeMeta);
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

        return fileStorage.renameFile(PathManager.getColumnPath(nameDB, tableName, columnName), changeColumnName) &&
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName));
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

    private boolean validateColumn(ColumnMetadata columnMetadata) {
        if (columnMetadata.getConstraints().contains(Constraints.AUTOINCREMENT)) {
            if (columnMetadata.getType() != DataType.INTEGER ||
                    !columnMetadata.getConstraints().contains(Constraints.PRIMARY_KEY)) {
                System.out.println("AUTOINCREMENT can only be used with INTEGER PRIMARY KEY");
                return false;
            }
        }
        return true;
    }

    private void updateTableMetadata(TableMetadata tableMetadata, ColumnMetadata columnMetadata) {
        tableMetadata.setColumnCount(tableMetadata.getColumnCount() + 1);
        tableMetadata.addColumnName(columnMetadata.getName());

        if (columnMetadata.getConstraints().contains(Constraints.AUTOINCREMENT)) {
            tableMetadata.addAutoIncrement();
        }

        if (columnMetadata.getConstraints().contains(Constraints.DEFAULT)) {
            tableMetadata.addDefault();
        }
    }

    private boolean writeColumnFiles(String tableName, ColumnMetadata columnMetadata, Column column, TableMetadata tableMetadata) {
        String columnPath = PathManager.getColumnPath(nameDB, tableName, columnMetadata.getName());
        String columnMetadataPath = PathManager.getColumnMetadataPath(nameDB, tableName, columnMetadata.getName());
        String tableMetadataPath = PathManager.getTableMetadataPath(nameDB, tableName);

        try {
            fileStorage.writeFile(columnPath, column);
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            fileStorage.writeFile(columnMetadataPath, columnMetadata);
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            fileStorage.writeFile(tableMetadataPath, tableMetadata);
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }
}