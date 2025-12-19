package FileWork;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.DatabaseMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;

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

    public boolean createDB(String name) throws FileStorageException {
        Date current = new Date();
        DatabaseMetadata databaseMetadata = new DatabaseMetadata(name, "1.0.0", "UTF-16", "Default", current, current);

        fileStorage.createDirectory(PathManager.getDatabasePath(name));

        fileStorage.writeFile(PathManager.getDatabaseMetadataPath(name), databaseMetadata);

        return true;
    }

    public boolean dropDB(String name) throws FileStorageException {
        if (Objects.equals(nameDB, name)) {
            nameDB = NO_USE_DB;
        }

        fileStorage.deleteDirectory(PathManager.getDatabasePath(name));

        return true;
    }

    public boolean useDB(String nameDB) {
        return setNameDB(nameDB);
    }

    public DatabaseMetadata loadDatabaseMetadata() throws FileStorageException {
        return fileStorage.readFile(PathManager.getDatabasePath(nameDB), DatabaseMetadata.class);
    }

    public boolean createTable(String tableName) throws  FileStorageException {
        TableMetadata tableMetadata = new TableMetadata(tableName, 0, 0, 0);

        fileStorage.createDirectory(PathManager.getTablePath(nameDB, tableName));

        fileStorage.writeFile(
                PathManager.getTableMetadataPath(nameDB, tableName),
                tableMetadata
        );
        return true;
    }

    public boolean dropTable(String tableName) throws FileStorageException {
        fileStorage.deleteDirectory(PathManager.getTablePath(nameDB, tableName));
        return true;
    }


    public boolean renameTable(String tableName, String changeTableName) throws FileStorageException {
        TableMetadata toChange = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        toChange.setName(changeTableName);

        fileStorage.renameDirectory(PathManager.getTablePath(nameDB, tableName), PathManager.getTablePath(nameDB, changeTableName));

        fileStorage.deleteFile(PathManager.getTableMetadataPath(nameDB, changeTableName));

        fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, changeTableName), toChange);

        return true;

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

    public boolean saveColumn(String tableName, String columnName, Column column) throws FileStorageException {
        fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnName), column);
        return true;
    }

    public ColumnMetadata loadColumnMetadata(String tableName, String columnName) throws FileStorageException {
        return fileStorage.readFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
    }

    public boolean saveColumnMetadata(String tableName, String columnName, ColumnMetadata columnMetadata) throws FileStorageException {
        fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), columnMetadata);
        return true;
    }

    public boolean deleteColumn(String tableName, String columnName) throws FileStorageException {
        TableMetadata tableMetadata = fileStorage.readFile(
                PathManager.getTableMetadataPath(nameDB, tableName),
                TableMetadata.class
        );

        tableMetadata.setColumnCount(tableMetadata.getColumnCount() - 1);
        tableMetadata.deleteColumnName(columnName);

        fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);

        fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName));
        fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName));

        return true;
    }

    public boolean renameColumn(String tableName, String columnName, String changeColumnName) throws FileStorageException {
        ColumnMetadata toChangeMeta = fileStorage.readFile(PathManager.
                getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
        toChangeMeta.setName(changeColumnName);

        fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, changeColumnName), toChangeMeta);

        fileStorage.renameFile(PathManager.getColumnPath(nameDB, tableName, columnName), changeColumnName);

        fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName));

        return true;
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

    private boolean writeColumnFiles(String tableName, ColumnMetadata columnMetadata, Column column, TableMetadata tableMetadata) throws FileStorageException {
        String columnPath = PathManager.getColumnPath(nameDB, tableName, columnMetadata.getName());
        String columnMetadataPath = PathManager.getColumnMetadataPath(nameDB, tableName, columnMetadata.getName());
        String tableMetadataPath = PathManager.getTableMetadataPath(nameDB, tableName);

        fileStorage.writeFile(columnPath, column);

        fileStorage.writeFile(columnMetadataPath, columnMetadata);

        fileStorage.writeFile(tableMetadataPath, tableMetadata);

        return true;
    }
}