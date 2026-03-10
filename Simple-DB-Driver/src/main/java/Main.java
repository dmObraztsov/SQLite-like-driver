import FileWork.FileManager;
import FileWork.JSON.JsonFileStorage;
import SqlParser.QueriesStruct.ExecutionResult;
import SqlParser.QueriesStruct.QueryInterface;
import SqlParser.Antlr.SQLProcessor;
import Yadro.DataStruct.DatabaseEngine;
import Yadro.DataStruct.Row;

import java.util.Scanner;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        JsonFileStorage storage = new JsonFileStorage();
        FileManager fileManager = new FileManager(storage);

        DatabaseEngine engine = new DatabaseEngine(fileManager);

        System.out.println("SQL Database Engine started. Enter your queries:");

        while (true) {
            System.out.print("> ");
            String inputLine = in.nextLine();

            if (inputLine == null || inputLine.equalsIgnoreCase("exit")) {
                break;
            }

            try {
                QueryInterface currentQuery = SQLProcessor.getQuery(inputLine);

                if (currentQuery == null) {
                    System.err.println("Syntax error: failed to parse query.");
                    continue;
                }
                ExecutionResult result = currentQuery.execute(engine);
                displayResult(result);

            } catch (Exception e) {
                System.err.println("Execution error: " + e.getMessage());
            }
        }
    }

    private static void displayResult(ExecutionResult result) {
        if (!result.isSuccess()) {
            System.err.println("Error: " + result.getMessage());
            return;
        }

        System.out.println(result.getMessage());

        List<Row> rows = result.getRows();
        if (rows != null && !rows.isEmpty()) {
            printTable(rows);
        }
    }

    private static void printTable(List<Row> rows) {
        for (Row row : rows) {
            System.out.println(row.toString());
        }
        System.out.println("Total rows: " + rows.size());
    }
}