package com.simpledb.jdbc;

import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.ExecutionResult;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import Yadro.DataStruct.Row;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Минимальная реализация DatabaseMetaData.
 * Spring Boot и HikariCP при старте вызывают несколько методов —
 * реализованы реально. Остальные возвращают безопасные заглушки.
 */
public class SimpleDatabaseMetaData implements DatabaseMetaData {

    private final SimpleConnection conn;
    private final String url;

    SimpleDatabaseMetaData(SimpleConnection conn, String url) {
        this.conn = conn;
        this.url  = url;
    }

    // ---- идентификация ----

    @Override public String getDatabaseProductName()    { return "SimpleDB"; }
    @Override public String getDatabaseProductVersion() { return "1.0"; }
    @Override public int getDatabaseMajorVersion()      { return 1; }
    @Override public int getDatabaseMinorVersion()      { return 0; }
    @Override public String getDriverName()             { return "SimpleDB JDBC Driver"; }
    @Override public String getDriverVersion()          { return "1.0"; }
    @Override public int getDriverMajorVersion()        { return 1; }
    @Override public int getDriverMinorVersion()        { return 0; }
    @Override public int getJDBCMajorVersion()          { return 4; }
    @Override public int getJDBCMinorVersion()          { return 2; }
    @Override public String getURL()                    { return url; }
    @Override public String getUserName()               { return ""; }
    @Override public String getIdentifierQuoteString()  { return "\""; }
    @Override public String getSQLKeywords()            { return ""; }
    @Override public String getExtraNameCharacters()    { return ""; }

    // ---- case sensitivity (HikariCP и Spring Boot смотрят на это) ----

    @Override public boolean storesUpperCaseIdentifiers()       { return false; }
    @Override public boolean storesLowerCaseIdentifiers()       { return false; }
    @Override public boolean storesMixedCaseIdentifiers()       { return true; }
    @Override public boolean supportsMixedCaseIdentifiers()     { return true; }
    @Override public boolean supportsMixedCaseQuotedIdentifiers() { return true; }
    @Override public boolean storesUpperCaseQuotedIdentifiers() { return false; }
    @Override public boolean storesLowerCaseQuotedIdentifiers() { return false; }
    @Override public boolean storesMixedCaseQuotedIdentifiers() { return true; }

    // ---- возможности (минимальный набор) ----

    @Override public boolean isReadOnly()                            { return false; }
    @Override public boolean supportsTransactions()                  { return true; }
    @Override public boolean supportsMultipleTransactions()          { return false; }
    @Override public boolean supportsTransactionIsolationLevel(int l){ return l == Connection.TRANSACTION_READ_COMMITTED; }
    @Override public boolean supportsSelectForUpdate()               { return false; }
    @Override public boolean supportsStoredProcedures()              { return false; }
    @Override public boolean supportsSubqueriesInComparisons()       { return false; }
    @Override public boolean supportsSubqueriesInExists()            { return false; }
    @Override public boolean supportsSubqueriesInIns()               { return false; }
    @Override public boolean supportsSubqueriesInQuantifieds()       { return false; }
    @Override public boolean supportsCorrelatedSubqueries()          { return false; }
    @Override public boolean supportsOuterJoins()                    { return false; }
    @Override public boolean supportsFullOuterJoins()                { return false; }
    @Override public boolean supportsLimitedOuterJoins()             { return false; }
    @Override public boolean supportsOrderByUnrelated()              { return true; }
    @Override public boolean supportsGroupBy()                       { return true; }
    @Override public boolean supportsGroupByBeyondSelect()           { return true; }
    @Override public boolean supportsGroupByUnrelated()              { return true; }
    @Override public boolean supportsColumnAliasing()                { return false; }
    @Override public boolean supportsExpressionsInOrderBy()          { return false; }
    @Override public boolean supportsNonNullableColumns()            { return true; }
    @Override public boolean supportsAlterTableWithAddColumn()       { return true; }
    @Override public boolean supportsAlterTableWithDropColumn()      { return true; }
    @Override public boolean supportsPositionedDelete()              { return false; }
    @Override public boolean supportsPositionedUpdate()              { return false; }
    @Override public boolean supportsMultipleResultSets()            { return false; }
    @Override public boolean supportsGetGeneratedKeys()              { return true; }
    @Override public boolean supportsBatchUpdates()                  { return false; }
    @Override public boolean supportsSavepoints()                    { return false; }
    @Override public boolean supportsNamedParameters()               { return false; }
    @Override public boolean supportsMultipleOpenResults()           { return false; }
    @Override public boolean supportsResultSetType(int t)            { return t == ResultSet.TYPE_FORWARD_ONLY; }
    @Override public boolean supportsResultSetConcurrency(int t, int c) { return c == ResultSet.CONCUR_READ_ONLY; }
    @Override public boolean ownUpdatesAreVisible(int t)             { return false; }
    @Override public boolean ownDeletesAreVisible(int t)             { return false; }
    @Override public boolean ownInsertsAreVisible(int t)             { return false; }
    @Override public boolean othersUpdatesAreVisible(int t)          { return false; }
    @Override public boolean othersDeletesAreVisible(int t)          { return false; }
    @Override public boolean othersInsertsAreVisible(int t)          { return false; }
    @Override public boolean updatesAreDetected(int t)               { return false; }
    @Override public boolean deletesAreDetected(int t)               { return false; }
    @Override public boolean insertsAreDetected(int t)               { return false; }
    @Override public boolean supportsResultSetHoldability(int h)     { return h == ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public int getResultSetHoldability()                   { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    @Override public boolean locatorsUpdateCopy()                    { return false; }
    @Override public boolean supportsStatementPooling()              { return false; }
    @Override public boolean supportsStoredFunctionsUsingCallSyntax(){ return false; }
    @Override public RowIdLifetime getRowIdLifetime()                { return RowIdLifetime.ROWID_UNSUPPORTED; }
    @Override public int getSQLStateType()                           { return DatabaseMetaData.sqlStateSQL99; }
    @Override public boolean supportsConvert()                       { return false; }
    @Override public boolean supportsConvert(int from, int to)       { return false; }
    @Override public boolean supportsTableCorrelationNames()         { return false; }
    @Override public boolean supportsDifferentTableCorrelationNames(){ return false; }
    @Override public boolean supportsLikeEscapeClause()              { return true; }
    @Override public boolean supportsMinimumSQLGrammar()             { return true; }
    @Override public boolean supportsCoreSQLGrammar()                { return false; }
    @Override public boolean supportsExtendedSQLGrammar()            { return false; }
    @Override public boolean supportsANSI92EntryLevelSQL()           { return false; }
    @Override public boolean supportsANSI92IntermediateSQL()         { return false; }
    @Override public boolean supportsANSI92FullSQL()                 { return false; }
    @Override public boolean supportsIntegrityEnhancementFacility()  { return false; }
    @Override public boolean supportsCatalogsInDataManipulation()    { return false; }
    @Override public boolean supportsCatalogsInProcedureCalls()      { return false; }
    @Override public boolean supportsCatalogsInTableDefinitions()    { return false; }
    @Override public boolean supportsCatalogsInIndexDefinitions()    { return false; }
    @Override public boolean supportsCatalogsInPrivilegeDefinitions(){ return false; }
    @Override public boolean supportsSchemasInDataManipulation()     { return false; }
    @Override public boolean supportsSchemasInProcedureCalls()       { return false; }
    @Override public boolean supportsSchemasInTableDefinitions()     { return false; }
    @Override public boolean supportsSchemasInIndexDefinitions()     { return false; }
    @Override public boolean supportsSchemasInPrivilegeDefinitions() { return false; }
    @Override public boolean supportsOpenCursorsAcrossCommit()       { return false; }
    @Override public boolean supportsOpenCursorsAcrossRollback()     { return false; }
    @Override public boolean supportsOpenStatementsAcrossCommit()    { return true; }
    @Override public boolean supportsOpenStatementsAcrossRollback()  { return true; }
    @Override public boolean supportsUnion()                             { return false; }
    @Override public boolean supportsUnionAll()                          { return false; }
    @Override public boolean supportsDataDefinitionAndDataManipulationTransactions() { return false; }
    @Override public boolean supportsDataManipulationTransactionsOnly()  { return true; }
    @Override public boolean autoCommitFailureClosesAllResultSets()      { return false; }
    @Override public boolean generatedKeyAlwaysReturned()            { return true; }
    @Override public boolean dataDefinitionIgnoredInTransactions()   { return true; }
    @Override public boolean dataDefinitionCausesTransactionCommit() { return false; }
    @Override public boolean doesMaxRowSizeIncludeBlobs()            { return false; }
    @Override public boolean nullsAreSortedHigh()                    { return false; }
    @Override public boolean nullsAreSortedLow()                     { return true; }
    @Override public boolean nullsAreSortedAtStart()                 { return false; }
    @Override public boolean nullsAreSortedAtEnd()                   { return false; }
    @Override public boolean nullPlusNonNullIsNull()                 { return true; }
    @Override public boolean usesLocalFiles()                        { return true; }
    @Override public boolean usesLocalFilePerTable()                 { return true; }
    @Override public boolean allProceduresAreCallable()              { return false; }
    @Override public boolean allTablesAreSelectable()                { return true; }
    @Override public boolean isCatalogAtStart()                      { return true; }
    @Override public String getCatalogSeparator()                    { return "."; }
    @Override public String getCatalogTerm()                         { return "database"; }
    @Override public String getSchemaTerm()                          { return "schema"; }
    @Override public String getProcedureTerm()                       { return "procedure"; }

    // ---- лимиты (0 = без ограничений) ----

    @Override public int getMaxBinaryLiteralLength()        { return 0; }
    @Override public int getMaxCharLiteralLength()          { return 0; }
    @Override public int getMaxColumnNameLength()           { return 0; }
    @Override public int getMaxColumnsInGroupBy()           { return 0; }
    @Override public int getMaxColumnsInIndex()             { return 0; }
    @Override public int getMaxColumnsInOrderBy()           { return 0; }
    @Override public int getMaxColumnsInSelect()            { return 0; }
    @Override public int getMaxColumnsInTable()             { return 0; }
    @Override public int getMaxConnections()                { return 1; }
    @Override public int getMaxCursorNameLength()           { return 0; }
    @Override public int getMaxIndexLength()                { return 0; }
    @Override public int getMaxSchemaNameLength()           { return 0; }
    @Override public int getMaxProcedureNameLength()        { return 0; }
    @Override public int getMaxCatalogNameLength()          { return 0; }
    @Override public int getMaxRowSize()                    { return 0; }
    @Override public int getMaxStatementLength()            { return 0; }
    @Override public int getMaxStatements()                 { return 0; }
    @Override public int getMaxTableNameLength()            { return 0; }
    @Override public int getMaxTablesInSelect()             { return 0; }
    @Override public int getMaxUserNameLength()             { return 0; }
    @Override public int getDefaultTransactionIsolation()   { return Connection.TRANSACTION_READ_COMMITTED; }

    // ---- системные функции ----

    @Override public String getNumericFunctions()   { return ""; }
    @Override public String getStringFunctions()    { return ""; }
    @Override public String getSystemFunctions()    { return ""; }
    @Override public String getTimeDateFunctions()  { return ""; }
    @Override public String getSearchStringEscape() { return "\\"; }

    // ---- ResultSet-ы для метаданных схемы (пустые) ----

    private ResultSet emptyRs() {
        return new SimpleResultSet(new ExecutionResult(true, "", List.of()), null);
    }

    @Override public ResultSet getProcedures(String c, String s, String p)          throws SQLException { return emptyRs(); }
    @Override public ResultSet getProcedureColumns(String c, String s, String p, String col) throws SQLException { return emptyRs(); }
    @Override
    public ResultSet getTables(String c, String s, String tablePattern, String[] types) throws SQLException {
        try {
            List<String> tables = conn.getEngine().listTables();
            List<Row> rows = new ArrayList<>();
            for (String tbl : tables) {
                if (tablePattern != null && !tablePattern.isEmpty() && !tablePattern.equals("%")
                        && !tbl.equalsIgnoreCase(tablePattern)) continue;
                Map<String, String> vals = new LinkedHashMap<>();
                vals.put("TABLE_CAT",  null);
                vals.put("TABLE_SCHEM", null);
                vals.put("TABLE_NAME", tbl);
                vals.put("TABLE_TYPE", "TABLE");
                vals.put("REMARKS", null);
                vals.put("TYPE_CAT", null);
                vals.put("TYPE_SCHEM", null);
                vals.put("TYPE_NAME", null);
                vals.put("SELF_REFERENCING_COL_NAME", null);
                vals.put("REF_GENERATION", null);
                rows.add(new Row(vals));
            }
            return new SimpleResultSet(new ExecutionResult(true, "", rows), null);
        } catch (Exception e) {
            throw new SQLException("Failed to list tables: " + e.getMessage(), e);
        }
    }

    @Override public ResultSet getSchemas()                                          throws SQLException { return emptyRs(); }
    @Override public ResultSet getSchemas(String catalog, String schemaPattern)      throws SQLException { return emptyRs(); }
    @Override public ResultSet getCatalogs()                                         throws SQLException { return emptyRs(); }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        Map<String, String> vals = new LinkedHashMap<>();
        vals.put("TABLE_TYPE", "TABLE");
        return new SimpleResultSet(new ExecutionResult(true, "", List.of(new Row(vals))), null);
    }

    @Override
    public ResultSet getColumns(String c, String s, String tablePattern, String colPattern) throws SQLException {
        try {
            List<Row> rows = new ArrayList<>();
            List<String> tables = conn.getEngine().listTables();
            for (String tbl : tables) {
                if (tablePattern != null && !tablePattern.isEmpty() && !tablePattern.equals("%")
                        && !tbl.equalsIgnoreCase(tablePattern)) continue;
                List<ColumnMetadata> cols = conn.getEngine().getColumnsMetadata(tbl);
                for (int i = 0; i < cols.size(); i++) {
                    ColumnMetadata meta = cols.get(i);
                    if (colPattern != null && !colPattern.isEmpty() && !colPattern.equals("%")
                            && !meta.getName().equalsIgnoreCase(colPattern)) continue;
                    boolean isAutoInc = meta.getConstraints().contains(Constraints.AUTOINCREMENT);
                    boolean notNull   = meta.getConstraints().contains(Constraints.NOT_NULL)
                                     || meta.getConstraints().contains(Constraints.PRIMARY_KEY);
                    Map<String, String> vals = new LinkedHashMap<>();
                    vals.put("TABLE_CAT",          null);
                    vals.put("TABLE_SCHEM",        null);
                    vals.put("TABLE_NAME",         tbl);
                    vals.put("COLUMN_NAME",        meta.getName());
                    vals.put("DATA_TYPE",          String.valueOf(toJdbcType(meta.getType())));
                    vals.put("TYPE_NAME",          meta.getType() != null ? meta.getType().getSqlType() : "TEXT");
                    vals.put("COLUMN_SIZE",        "255");
                    vals.put("BUFFER_LENGTH",      null);
                    vals.put("DECIMAL_DIGITS",     null);
                    vals.put("NUM_PREC_RADIX",     "10");
                    vals.put("NULLABLE",           notNull ? String.valueOf(DatabaseMetaData.columnNoNulls)
                                                           : String.valueOf(DatabaseMetaData.columnNullable));
                    vals.put("REMARKS",            null);
                    vals.put("COLUMN_DEF",         meta.getDefaultValue());
                    vals.put("SQL_DATA_TYPE",      null);
                    vals.put("SQL_DATETIME_SUB",   null);
                    vals.put("CHAR_OCTET_LENGTH",  "255");
                    vals.put("ORDINAL_POSITION",   String.valueOf(i + 1));
                    vals.put("IS_NULLABLE",        notNull ? "NO" : "YES");
                    vals.put("SCOPE_CATALOG",      null);
                    vals.put("SCOPE_SCHEMA",       null);
                    vals.put("SCOPE_TABLE",        null);
                    vals.put("SOURCE_DATA_TYPE",   null);
                    vals.put("IS_AUTOINCREMENT",   isAutoInc ? "YES" : "NO");
                    vals.put("IS_GENERATEDCOLUMN", isAutoInc ? "YES" : "NO");
                    rows.add(new Row(vals));
                }
            }
            return new SimpleResultSet(new ExecutionResult(true, "", rows), null);
        } catch (Exception e) {
            throw new SQLException("Failed to get columns: " + e.getMessage(), e);
        }
    }

    private static int toJdbcType(DataType type) {
        if (type == null) return Types.VARCHAR;
        return switch (type) {
            case INTEGER -> Types.INTEGER;
            case REAL    -> Types.DOUBLE;
            case BLOB    -> Types.BLOB;
            case NULL    -> Types.NULL;
            default      -> Types.VARCHAR;
        };
    }
    @Override public ResultSet getColumnPrivileges(String c, String s, String t, String col) throws SQLException { return emptyRs(); }
    @Override public ResultSet getTablePrivileges(String c, String s, String t)     throws SQLException { return emptyRs(); }
    @Override public ResultSet getBestRowIdentifier(String c, String s, String t, int scope, boolean nullable) throws SQLException { return emptyRs(); }
    @Override public ResultSet getVersionColumns(String c, String s, String t)      throws SQLException { return emptyRs(); }
    @Override public ResultSet getPrimaryKeys(String c, String s, String t)         throws SQLException { return emptyRs(); }
    @Override public ResultSet getImportedKeys(String c, String s, String t)        throws SQLException { return emptyRs(); }
    @Override public ResultSet getExportedKeys(String c, String s, String t)        throws SQLException { return emptyRs(); }
    @Override public ResultSet getCrossReference(String pc, String ps, String pt, String fc, String fs, String ft) throws SQLException { return emptyRs(); }
    @Override public ResultSet getTypeInfo()                                         throws SQLException { return emptyRs(); }
    @Override public ResultSet getIndexInfo(String c, String s, String t, boolean unique, boolean approx) throws SQLException { return emptyRs(); }
    @Override public ResultSet getUDTs(String c, String s, String t, int[] types)   throws SQLException { return emptyRs(); }
    @Override public ResultSet getSuperTypes(String c, String s, String t)          throws SQLException { return emptyRs(); }
    @Override public ResultSet getSuperTables(String c, String s, String t)         throws SQLException { return emptyRs(); }
    @Override public ResultSet getAttributes(String c, String s, String t, String attr) throws SQLException { return emptyRs(); }
    @Override public ResultSet getClientInfoProperties()                             throws SQLException { return emptyRs(); }
    @Override public ResultSet getFunctions(String c, String s, String f)           throws SQLException { return emptyRs(); }
    @Override public ResultSet getFunctionColumns(String c, String s, String f, String col) throws SQLException { return emptyRs(); }
    @Override public ResultSet getPseudoColumns(String c, String s, String t, String col)   throws SQLException { return emptyRs(); }

    // ---- connection ----

    @Override public Connection getConnection() { return conn; }

    // ---- Wrapper ----

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
