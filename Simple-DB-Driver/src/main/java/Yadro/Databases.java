package Yadro;

import java.util.List;

public interface Databases {

    /**
     * Use "name" database
     * @param name - name of database
     * @return success of execution
     */
    boolean USE(String name);

    /**
     * Create database with "name"
     * @param name - name of database
     * @return success of execution
     */
    boolean CREATE_DATABASE(String name);

    /**
     * Delete database with "name"
     * @param name - name of database
     * @return success of execution
     */
    boolean DROP_DATABASE(String name);

    /**
     * Create table with "name"
     * @param name - name of table
     * @return success of execution
     */
    boolean CREATE_TABLE(String name);

    /**
     * Delete table with "name"
     * @param name - name of table
     * @return success of execution
     */
    boolean DROP_TABLE(String name);

    /**
     * Finds out if the database exists
     * @param name - name of database
     * @return search result
     */
    boolean IF_EXIST(String name);

    /**
     * Show all tables in database
     * @return list of tables
     */
    List<Table> SHOW_TABLES();
}
