package Yadro.DataStruct;

public enum DataType {
    NULL("NULL", Void.class, -1),
    INTEGER("INTEGER", Long.class, 8),
    REAL("REAL", Double.class, 8),
    TEXT("TEXT", String.class, 255); // default size

    private final String sqlType;
    private final Class<?> javaType;
    private final int defaultSize;

    DataType(String sqlType, Class<?> javaType, int defaultSize) {
        this.sqlType = sqlType;
        this.javaType = javaType;
        this.defaultSize = defaultSize;
    }

    public String getSqlType() { return sqlType; }
    public Class<?> getJavaType() { return javaType; }
    public int getDefaultSize() { return defaultSize; }
}