package SqlParser.QueriesStruct;

import FileWork.FileManager;

public interface QueryInterface {
    boolean execute(FileManager fileManager);

    String getStringVision();
}
