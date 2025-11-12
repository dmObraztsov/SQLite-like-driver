package FileWork;

import Yadro.DataStruct.Column;

public class FileManager {
    private final FileStorage fileStorage;
    private String nameDB;

    public FileManager(FileStorage someStorage, String nameDB)
    {
        this.nameDB = nameDB;
        fileStorage = someStorage;
    }

    public void createDB(String name)
    {
        fileStorage.createDirectory(name); //TODO Hande file errors
    }

    public void dropDB(String name)
    {
        fileStorage.deleteDirectory(name);
    }

    public void createTable(String tableName)
    {
        fileStorage.createDirectory('/' + nameDB + '/' + tableName); //TODO Hande file errors
    }

//    public Column loadColumn(String tableName, String columnName)
//    {
//        String result;
//        Column column;
//        try
//        {
//            result = fileStorage.readFile(nameDB + '/' + tableName + '/' + columnName);
//            //TODO Parse "result" with Jackson
//            column = new Column();
//        } catch (Exception e)
//        {
//            System.out.println(e.getMessage());
//        }//TODO Handle file errors
//
//        return column
//    }
//
//    public void saveColumn(String tableName, Column column)
//    {
//        try
//        {
//            fileStorage.createDirectory(nameDB + '/');
//        } catch () //TODO Handle file errors
//    }
//
    public String getNameDB()
    {
        return nameDB;
    }

    public void setNameDB(String nameDB)
    {
        if(fileStorage.exists(nameDB))
        {
            this.nameDB = nameDB;
        }

        else
        {
            System.out.println("No such Database");
        }
    }
}