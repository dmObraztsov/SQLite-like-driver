package FileWork;

public class PathManager {
    static private String basePath = "src/main/data";

    public static void setBasePath(String newPath) {
        if (newPath.endsWith("/") || newPath.endsWith("\\")) {
            basePath = newPath.substring(0, newPath.length() - 1);
        } else {
            basePath = newPath;
        }
    }

    public static void reset() {
        basePath = "src/main/data";
    }

    public static String getDatabasePath(String dataBaseName) {
        return basePath + "/" + dataBaseName;
    }

    public static String getDatabaseMetadataPath(String dataBaseName) {
        return getDatabasePath(dataBaseName) + '/' + "metadata.bin";
    }

    public static String getTablePath(String dataBasename, String tableName) {
        return getDatabasePath(dataBasename) + '/' + "tables" + '/' + tableName;
    }

    public static String getTableMetadataPath(String dataBaseName, String tableName) {
        return getTablePath(dataBaseName, tableName) + '/' + "metadata.bin";
    }

    public static String getTableDataPath(String dataBaseName, String tableName) {
        return getTablePath(dataBaseName, tableName) + '/' + "data/";
    }

    public static String getColumnPath(String dataBaseName, String tableName, String columnName) {
        return getTableDataPath(dataBaseName, tableName) + columnName + ".bin";
    }

    public static String getColumnMetadataPath(String dataBaseName, String tableName, String columnName) {
        return getTableDataPath(dataBaseName, tableName) + columnName + ".metadata.bin";
    }

    public static String getWalDir(String dbName) {
        return getDatabasePath(dbName) + "/wal";
    }

    public static String getWalTmpPath(String dbName, String txId) {
        return getWalDir(dbName) + "/" + txId + ".wal.tmp";
    }

    public static String getWalPath(String dbName, String txId) {
        return getWalDir(dbName) + "/" + txId + ".wal";
    }

    public static String getIndexDir(String dbName, String tableName) {
        return getTablePath(dbName, tableName) + "/index";
    }

    public static String getIndexPath(String dbName, String tableName, String columnName) {
        return getIndexDir(dbName, tableName) + "/" + columnName + ".index.bin";
    }
}
