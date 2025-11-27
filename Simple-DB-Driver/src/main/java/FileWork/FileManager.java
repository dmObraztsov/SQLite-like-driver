package FileWork;

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

    public DatabaseMetadata loadDatabaseMetadata() {
        return fileStorage.readFile(PathManager.getDatabasePath(nameDB), DatabaseMetadata.class);
    }

    public boolean createTable(String tableName) {
        TableMetadata tableMetadata = new TableMetadata(tableName, 0, 0);
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

    public TableMetadata loadTableMetadata(String tableName) {
        return fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
    }

    public Column loadColumn(String tableName, String columnName) {
        return fileStorage.readFile(PathManager.getColumnPath(nameDB, tableName, columnName), Column.class);
    }

    public boolean createColumn(String tableName, ColumnMetadata columnMetadata) {
        if (columnMetadata == null) {
            System.out.println("Column cannot be null");
            return false;
        }

        Column column = new Column();
        TableMetadata tableMetadata = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        tableMetadata.setColumnCount(tableMetadata.getColumnCount() + 1);
        tableMetadata.addColumnName(columnMetadata.getName());
        if(columnMetadata.getConstraints().contains(Constraints.PRIMARY_KEY)) tableMetadata.addPrimaryKey();
        if(columnMetadata.getConstraints().contains(Constraints.AUTOINCREMENT)) {
            if(columnMetadata.getType() != DataType.INTEGER || !columnMetadata.getConstraints().contains(Constraints.PRIMARY_KEY)) {
                return false;
            }
        }

        return fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnMetadata.getName()), column) &
                fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnMetadata.getName()), columnMetadata) &
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }

    public boolean createPrimaryKeyMap(String tableName) {
        if(!new File(PathManager.getTableDataPath(nameDB, tableName)).exists())
        {
            fileStorage.createDirectory(PathManager.getTableDataPath(nameDB, tableName));
        }

        return fileStorage.writeFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), new PrimaryKeyMap());
    }

    public boolean saveColumn(String tableName, String columnName, Column column){
        return fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnName), column);
    }

    public ColumnMetadata loadColumnMetadata(String tableName, String columnName) {
        return fileStorage.readFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
    }

    public boolean saveColumnMetadata(String tableName, String columnName, ColumnMetadata columnMetadata)
    {
        return fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), columnMetadata);
    }

    public boolean savePrimaryKeyMap(String tableName, PrimaryKeyMap primaryKeyMap)
    {
        return fileStorage.writeFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), primaryKeyMap);
    }

    public PrimaryKeyMap loadPrimaryKeyMap(String tableName)
    {
        return fileStorage.readFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), PrimaryKeyMap.class);
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
        ColumnMetadata toChangeMeta = fileStorage.readFile(PathManager.
                getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
        toChangeMeta.setName(changeColumnName);
        return fileStorage.renameFile(PathManager.getColumnPath(nameDB, tableName, columnName), changeColumnName) &
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName)) &
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