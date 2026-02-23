package SqlParser.QueriesStruct;

import Exceptions.FileStorageException;
import Yadro.DataStruct.DatabaseEngine;

public interface QueryInterface {
    ExecutionResult execute(DatabaseEngine engine) throws Exception;
}