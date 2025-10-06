package Yadro;

import java.util.List;

public interface Table {

    /**
     * Change table
     * @param tableName name of table
     * @return success of execution
     */
    boolean ALTER(String tableName);

    /**
     * Update lines in table
     * @param tableName name of table
     * @param columns list of columns
     * @return success of execution
     */
    boolean UPDATE(List<String> columns, String tableName);

    /**
     * Insert lines in table
     * @param tableName name of table
     * @param columns list of columns
     * @return success of execution
     */
    boolean INSERT(List<String> columns, String tableName);

    /**
     * Select line from table
     * @param tableName name of table
     * @param columns list of columns
     * @return success of execution
     */
    boolean SELECT(List<String> columns, String tableName);

    /**
     * Delete lines from table
     * @param tableName name of table
     * @param columns list of columns
     * @return success of execution
     */
    boolean DELETE(List<String> columns, String tableName);
}
