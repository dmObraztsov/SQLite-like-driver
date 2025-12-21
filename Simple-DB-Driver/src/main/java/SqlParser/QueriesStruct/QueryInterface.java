package SqlParser.QueriesStruct;

import Exceptions.FileStorageException;
import FileWork.FileManager;

public interface QueryInterface {
    boolean execute(FileManager fileManager) throws FileStorageException;
}
