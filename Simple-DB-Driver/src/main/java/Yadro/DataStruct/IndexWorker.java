package Yadro.DataStruct;

import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import SqlParser.QueriesStruct.Queries;

import java.util.ArrayList;

public class IndexWorker {
    static public boolean AddToDataBase(ArrayList<Queries.InsertTableQuery.ColumnValue> row, FileManager fileManager, String tableName) {
        int index = getIndexToAdd(fileManager, tableName);
        boolean flag = true;

        for(Queries.InsertTableQuery.ColumnValue curr : row) {
            if(curr.isPrimaryKey()) {
                addToIdMap(fileManager, tableName, curr.value(), index);
            }

            Column column = fileManager.loadColumn(tableName, curr.column());
            ColumnMetadata columnMetadata = fileManager.loadColumnMetadata(tableName, curr.column());
            column.addData(curr.value(), index);
            columnMetadata.incrementSize();

            flag = fileManager.saveColumn(tableName, curr.column(), column) &&
                    fileManager.saveColumnMetadata(tableName, curr.column(), columnMetadata);

            if(!flag) return flag;
        }

        return flag;
    }

    private static int getIndexToAdd(FileManager fileManager, String tableName) {
        int result = 1;
        Column _id = fileManager.loadColumn(tableName, "_id");
        if(!_id.getData().isEmpty()) result = Integer.parseInt(_id.getData().getLast()) + 1;
        _id.addData(String.valueOf(result), _id.getData().size());

        fileManager.saveColumn(tableName, "_id", _id);
        return result;
    }

    private static void addToIdMap(FileManager fileManager, String tableName, Object key, int index) {
        PrimaryKeyMap primaryKeyMap = fileManager.loadPrimaryKeyMap(tableName);
        primaryKeyMap.addLink(key, index);

        fileManager.savePrimaryKeyMap(tableName, primaryKeyMap);
    }
}
