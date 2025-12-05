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

    /*
    public boolean createDB(String name) {
        try {
            Date current = new Date();
            DatabaseMetadata databaseMetadata = new DatabaseMetadata(name, "1.0.0", "UTF-16", "Default", current, current);
            return fileStorage.createDirectory(PathManager.getDatabasePath(name)) &&
                    fileStorage.writeFile(PathManager.getDatabaseMetadataPath(name), databaseMetadata);
        } catch (FileStorageException ex) {

        }
    }
    */

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

    /*
    public boolean createTable(String tableName) {
        TableMetadata tableMetadata = new TableMetadata(tableName, 0, 0, 0);
        return fileStorage.createDirectory(PathManager.getTablePath(nameDB, tableName)) &&
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }
     */

    public boolean dropTable(String tableName) {
        return fileStorage.deleteDirectory(PathManager.getTablePath(nameDB, tableName));
    }


    public boolean renameTable(String tableName, String changeTableName) {
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

    /*
    public boolean renameTable(String tableName, String changeTableName) {
        TableMetadata toChange = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        toChange.setName(changeTableName);

        return fileStorage.renameDirectory(PathManager.getTablePath(nameDB, tableName), PathManager.getTablePath(nameDB, changeTableName)) &&
                fileStorage.deleteFile(PathManager.getTableMetadataPath(nameDB, changeTableName)) &&
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, changeTableName), toChange);
    }
     */

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

    public void createId(String tableName) throws FileStorageException {
        Column _id = new Column();
        fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, "_id"), _id);
    }

    public boolean createPrimaryKeyMap(String tableName) {
        if(!new File(PathManager.getTableDataPath(nameDB, tableName)).exists())
        {
            fileStorage.createDirectory(PathManager.getTableDataPath(nameDB, tableName));
        }

        try {
            fileStorage.writeFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), new PrimaryKeyMap());
            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }

    }

    /*
    public boolean createPrimaryKeyMap(String tableName) {
        if(!new File(PathManager.getTableDataPath(nameDB, tableName)).exists())
        {
            fileStorage.createDirectory(PathManager.getTableDataPath(nameDB, tableName));
        }

        return fileStorage.writeFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), new PrimaryKeyMap());
    }
    */

    public boolean saveColumn(String tableName, String columnName, Column column){
        try {
            fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnName), column);
            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /*
    public boolean saveColumn(String tableName, String columnName, Column column){
        return fileStorage.writeFile(PathManager.getColumnPath(nameDB, tableName, columnName), column);
    }
     */

    public ColumnMetadata loadColumnMetadata(String tableName, String columnName) {
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

    /*
    public boolean saveColumnMetadata(String tableName, String columnName, ColumnMetadata columnMetadata)
    {
        return fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName), columnMetadata);
    }

     */

    public boolean savePrimaryKeyMap(String tableName, PrimaryKeyMap primaryKeyMap)
    {
        try {
            fileStorage.writeFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), primaryKeyMap);
            return true;
        } catch (FileStorageException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /*
    public boolean savePrimaryKeyMap(String tableName, PrimaryKeyMap primaryKeyMap)
    {
        return fileStorage.writeFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), primaryKeyMap);
    }

     */

    public PrimaryKeyMap loadPrimaryKeyMap(String tableName)
    {
        return fileStorage.readFile(PathManager.getTableDataIDMapColumnsPath(nameDB, tableName), PrimaryKeyMap.class);
    }

    //этот метод обновлю, когда допишу другие классы исключения и изменю соответствующие методы
    public boolean deleteColumn(String tableName, String columnName) {
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

    /*
    public boolean deleteColumn(String tableName, String columnName) {
        TableMetadata tableMetadata = fileStorage.readFile(PathManager.getTableMetadataPath(nameDB, tableName), TableMetadata.class);
        tableMetadata.setColumnCount(tableMetadata.getColumnCount() - 1);
        tableMetadata.deleteColumnName(columnName);

        return fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnName)) &&
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName)) &&
                fileStorage.writeFile(PathManager.getTableMetadataPath(nameDB, tableName), tableMetadata);
    }

     */

    public boolean renameColumn(String tableName, String columnName, String changeColumnName) {
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


    /*
    public boolean renameColumn(String tableName, String columnName, String changeColumnName) {
        ColumnMetadata toChangeMeta = fileStorage.readFile(PathManager.
                getColumnMetadataPath(nameDB, tableName, columnName), ColumnMetadata.class);
        toChangeMeta.setName(changeColumnName);
        return fileStorage.renameFile(PathManager.getColumnPath(nameDB, tableName, columnName), changeColumnName) &&
                fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnName)) &&
                fileStorage.writeFile(PathManager.getColumnMetadataPath(nameDB, tableName, changeColumnName), toChangeMeta);
    }

     */

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


    /*
    private boolean writeColumnFiles(String tableName, ColumnMetadata columnMetadata, Column column, TableMetadata tableMetadata) {
        String columnPath = PathManager.getColumnPath(nameDB, tableName, columnMetadata.getName());
        String columnMetadataPath = PathManager.getColumnMetadataPath(nameDB, tableName, columnMetadata.getName());
        String tableMetadataPath = PathManager.getTableMetadataPath(nameDB, tableName);

        boolean columnWritten = fileStorage.writeFile(columnPath, column);
        boolean metadataWritten = fileStorage.writeFile(columnMetadataPath, columnMetadata);
        boolean tableMetadataWritten = fileStorage.writeFile(tableMetadataPath, tableMetadata);

        if (!(columnWritten && metadataWritten && tableMetadataWritten)) {
            rollbackColumnCreation(tableName, columnMetadata, columnWritten, metadataWritten);
            return false;
        }

        return true;
    }

     */

    private void rollbackColumnCreation(String tableName, ColumnMetadata columnMetadata,
                                        boolean columnWritten, boolean metadataWritten) {
        if (columnWritten) {
            fileStorage.deleteFile(PathManager.getColumnPath(nameDB, tableName, columnMetadata.getName()));
        }
        if (metadataWritten) {
            fileStorage.deleteFile(PathManager.getColumnMetadataPath(nameDB, tableName, columnMetadata.getName()));
        }
        System.out.println("Failed to create column: " + columnMetadata.getName());
    }
}