import Exceptions.FileStorageException;
import FileWork.FileManager;
import FileWork.JSON.JsonFileStorage;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import SqlParser.Antlr.SQLProcessor;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileStorageException {
        Scanner in = new Scanner(System.in); //TODO Handle input errors
        FileManager fileManager = new FileManager(new JsonFileStorage(), "testDB");
        String inputLine;

        while((inputLine = in.nextLine()) != null)
        {
            QueryInterface currentQuery = SQLProcessor.getQuery(inputLine);
            if(currentQuery == null)
            {
                System.out.println("Syntax error");
                continue;
            }

            if(fileManager.getNameDB().isEmpty() && !(currentQuery instanceof Queries.CreateDataBaseQuery) &&
                    !(currentQuery instanceof Queries.UseDataBaseQuery) &&
                    !(currentQuery instanceof Queries.DropDataBaseQuery))
            {
                System.out.println("DataBase is not exits or not connected");
                continue;
            }

            if(!currentQuery.execute(fileManager)) //TODO Нужно ограничить возможность изменять и
                // удалять внутренние системные файлы
            {
                System.out.println("Error in query: " + currentQuery.getClass());
            }
        }
    }
}
