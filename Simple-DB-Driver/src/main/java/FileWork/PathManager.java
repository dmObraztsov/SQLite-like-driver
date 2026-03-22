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
