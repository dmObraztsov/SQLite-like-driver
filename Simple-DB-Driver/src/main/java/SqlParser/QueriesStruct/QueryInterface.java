package SqlParser.QueriesStruct;

import FileWork.FileManager;

public interface QueryInterface {
    void execute(FileManager fileManager);

    String getStringVision();
}
