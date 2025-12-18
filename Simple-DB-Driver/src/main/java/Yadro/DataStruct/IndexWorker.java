package Yadro.DataStruct;

import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;

import java.util.ArrayList;

public class IndexWorker {
    static public boolean AddToDataBase(ArrayList<Queries.InsertTableQuery.ColumnValue> row, FileManager fileManager, String tableName) {
        boolean flag = true;

        for(Queries.InsertTableQuery.ColumnValue curr : row) {
            Column column = fileManager.loadColumn(tableName, curr.column());
            ColumnMetadata columnMetadata = fileManager.loadColumnMetadata(tableName, curr.column());
            column.addData(curr.value());
            columnMetadata.incrementSize();

            flag = fileManager.saveColumn(tableName, curr.column(), column) &&
                    fileManager.saveColumnMetadata(tableName, curr.column(), columnMetadata);

            if(!flag) return flag;
        }

        return flag;
    }
}
