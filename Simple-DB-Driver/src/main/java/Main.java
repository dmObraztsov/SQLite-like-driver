import FileWork.FileManager;
import FileWork.JsonFileStorage;
import SqlParser.QueriesStruct.Queries;
import SqlParser.QueriesStruct.QueryInterface;
import SqlParser.Antlr.SQLProcessor;

import java.util.Scanner;

public class Main {
    public static void main(String[] args)
    {
        Scanner in  = new Scanner(System.in); //TODO Handle input errors
        String inputLine;

        FileManager fileManager = new FileManager(new JsonFileStorage(), "");
        while((inputLine = in.nextLine()) != null)
        {
            QueryInterface currentQuery = SQLProcessor.getQuery(inputLine); //TODO Handle parsing errors
            if(fileManager.getNameDB().isEmpty() && !(currentQuery instanceof Queries.CreateDataBaseQuery) &&
                    !(currentQuery instanceof Queries.UseDataBaseQuery))
            {
                System.out.println("DB is not exist");
                continue;
            }

            currentQuery.execute(fileManager);
        }
    }
}
