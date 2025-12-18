package FileWork;

public class PathManager {
    static private final String BASE_PATH = "src/main/data/";

    public static String getDatabasePath(String dataBaseName) {
        return BASE_PATH + dataBaseName;
    }

    public static String getDatabaseMetadataPath(String dataBaseName) {
        return getDatabasePath(dataBaseName) + '/' + "metadata.json";
    }

    public static String getTablePath(String dataBasename, String tableName) {
        return getDatabasePath(dataBasename) + '/' + "tables" + '/' + tableName;
    }

    public static String getTableMetadataPath(String dataBaseName, String tableName) {
        return getTablePath(dataBaseName, tableName) + '/' + "metadata.json";
    }

    public static String getTableDataPath(String dataBaseName, String tableName) {
        return getTablePath(dataBaseName, tableName) + '/' + "data/";
    }

    public static String getColumnPath(String dataBaseName, String tableName, String columnName) {
        return getTableDataPath(dataBaseName, tableName) + columnName + ".json";
    }

    public static String getColumnMetadataPath(String dataBaseName, String tableName, String columnName) {
        return getTableDataPath(dataBaseName, tableName) + columnName + ".metadata.json";
    }
}
