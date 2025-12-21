package SqlParser.QueriesStruct;

import Exceptions.*;
import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Queries {

    public static class CreateDataBaseQuery implements QueryInterface
    {
        private final String databaseName;

        public CreateDataBaseQuery(String databaseName) {
            this.databaseName = databaseName;
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException {
            return fileManager.createDB(databaseName);
        }
    }

    public static class DropDataBaseQuery implements QueryInterface
    {
        private final String databaseName;

        public DropDataBaseQuery(String databaseName)
        {
            this.databaseName = databaseName;
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException {
            return fileManager.dropDB(databaseName);
        }
    }

    public static class UseDataBaseQuery implements QueryInterface
    {
        private final String databaseName;

        public UseDataBaseQuery(String databaseName)
        {
            this.databaseName = databaseName;
        }

        @Override
        public boolean execute(FileManager fileManager) {
            return fileManager.useDB(databaseName);
        }
    }

    public static class CreateTableQuery implements QueryInterface
    {
        private final String tableName;
        private final ArrayList<ColumnMetadata> tableColumns = new ArrayList<>();

        public CreateTableQuery(String tableName, ArrayList<ColumnMetadata> tableColumns) {
            this.tableName = tableName;
            this.tableColumns.addAll(tableColumns);
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException {
            boolean flag;

            flag = fileManager.createTable(tableName);

            if (!flag) return false;

            for (ColumnMetadata curr : tableColumns) {
                try {
                    fileManager.createColumn(tableName, curr);
                } catch (FileStorageException e) {
                    fileManager.dropTable((tableName));
                    System.err.println(e.getMessage());
                    return false;
                }
            }
            return true;
        }
    }

    public static class DropTableQuery implements QueryInterface
    {
        private final String tableName;

        public DropTableQuery(String tableName)
        {
            this.tableName = tableName;
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException {
            return fileManager.dropTable(tableName);
        }
    }

    public static class AlterTableQuery implements QueryInterface
    {
        protected final String tableName;

        public AlterTableQuery(String tableName)
        {
            this.tableName = tableName;
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException {
            return false;
        }

        public static class AlterRenameTableQuery extends AlterTableQuery
        {
            private final String changeTableName;

            public AlterRenameTableQuery(String tableName, String changeTableName) {
                super(tableName);
                this.changeTableName = changeTableName;
            }

            @Override
            public boolean execute(FileManager fileManager) throws FileStorageException {
                return fileManager.renameTable(super.tableName, this.changeTableName);
            }
        }

        public static class AlterAddColumnQuery extends AlterTableQuery
        {
            private final ColumnMetadata column;

            public AlterAddColumnQuery(String tableName, ColumnMetadata column) {
                super(tableName);
                this.column = column;
            }

            @Override
            public boolean execute(FileManager fileManager) throws FileStorageException {
                return fileManager.createColumn(super.tableName, this.column);
            }
        }

        public static class AlterDropColumnQuery extends AlterTableQuery
        {
            private final String dropColumnName;

            public AlterDropColumnQuery(String tableName, String dropColumnName) {
                super(tableName);
                this.dropColumnName = dropColumnName;
            }

            @Override
            public boolean execute(FileManager fileManager) throws FileStorageException {
                return fileManager.deleteColumn(super.tableName, this.dropColumnName);
            }
        }

        public static class AlterRenameColumnQuery extends AlterTableQuery
        {
            private final String renameColumnName;
            private final String columnName;

            public AlterRenameColumnQuery(String tableName, String columnName, String renameColumnName) {
                super(tableName);
                this.renameColumnName = renameColumnName;
                this.columnName = columnName;
            }

            @Override
            public boolean execute(FileManager fileManager) throws FileStorageException {
                return fileManager.renameColumn(super.tableName, columnName, renameColumnName);
            }
        }
    }

    public static class InsertTableQuery implements QueryInterface
    {
        private final String tableName;
        private ArrayList<String> columns;
        private final ArrayList<String> values;

        public InsertTableQuery(String tableName, ArrayList<String> columns, ArrayList<String> values)
        {
            this.tableName = tableName;
            this.columns = new ArrayList<>(columns);
            this.values = new ArrayList<>(values);
        }

        @Override
        public boolean execute(FileManager fileManager){

            try {
                TableMetadata tableMetadata = fileManager.loadTableMetadata(tableName);
                columns = new ArrayList<>(tableMetadata.getColumnNames());

                int index = 0;
                int j = 0;

                for(String curr : columns) {
                    ColumnMetadata  columnMetadata = fileManager.loadColumnMetadata(tableName, curr);
                    if(columnMetadata.getSize() > index) index = columnMetadata.getSize();
                }

                for(String curr : columns) {
                    Column column = fileManager.loadColumn(tableName, curr);
                    ColumnMetadata columnMetadata = fileManager.loadColumnMetadata(tableName, curr);

                    String value;
                    value = values.get(j++);

                    if(fullCheck(column, columnMetadata, value)) {
                        columnMetadata.incrementSize();
                        column.addData(index, value);

                        fileManager.saveColumnMetadata(tableName, curr, columnMetadata);
                        fileManager.saveColumn(tableName, curr, column);
                    }
                }

                return true;
            } catch (FileStorageException e) {
                System.out.println(e.getMessage());
                return false;
            }
        }

        private boolean fullCheck(Column column, ColumnMetadata columnMetadata, String content) {
            return (providedType(content) == columnMetadata.getType()) && checkConstraints(columnMetadata, content);
        }

        private DataType providedType(String content) {
            if(content.equals("NULL")) return DataType.NULL;
            if(content.matches("-?\\d+")) return DataType.INTEGER;
            if(content.matches("-?\\d+(\\.\\d+)?")) return DataType.REAL;
            if(content.startsWith("\"") && content.endsWith("\"") && content.length() >= 2) return DataType.TEXT;
            else return null;
        }

        private boolean checkConstraints(ColumnMetadata columnMetadata, String content) {
            return true;
        }
    }

    public static class SelectDataQuery implements QueryInterface {
        private final List<String>                 selectCols;
        private final boolean                      isStar;
        private final String                       tableName;
        private final String                       whereName;
        private final String                       whereValue;

        public SelectDataQuery(List<String> selectCols, Boolean isStar, String tableName, String whereName, String whereValue) {
            this.selectCols = selectCols;
            this.tableName = tableName;
            this.whereName = whereName;
            this.whereValue = whereValue;
            this.isStar = isStar;
        }

        @Override
        public boolean execute(FileManager fileManager){
            try {
                TableMetadata tableMetadata              =   fileManager.loadTableMetadata(tableName);
                List<String>  tableMetadataColumnNames   =   tableMetadata.getColumnNames();

                if(tableMetadataColumnNames.isEmpty()) {
                    return false;
                }
                List<Integer> indicesOfSelectedRows = new ArrayList<>();
                List<Column> columns = new ArrayList<>();
                List<Column> selectedColumns = new ArrayList<>();

                for(String columnName : tableMetadataColumnNames) {
                    columns.add(fileManager.loadColumn(tableName, columnName));
                }

                int columnSize = columns.getFirst().getData().size();

                if(isStar) {
                    selectedColumns.addAll(columns);
                    IntStream.range(0, columnSize).forEach(indicesOfSelectedRows::add);
                }

                else {
                    for(String columnName : selectCols) {
                        if(!tableMetadataColumnNames.contains(columnName)) {
                            return false;
                        }
                    }

                    if (whereName != null && whereValue != null) {
                        Column whereColumn = fileManager.loadColumn(tableName, whereName);

                        ArrayList<String> whereColumnData = whereColumn.getData();
                        for(int i = 0; i < columnSize; i++) {
                            if(whereColumnData.get(i).equals(whereValue)) {
                                indicesOfSelectedRows.add(i);
                            }
                        }
                    }
                    else IntStream.range(0, columnSize).forEach(indicesOfSelectedRows::add);

                    for(String columnName : selectCols) {
                        selectedColumns.add(fileManager.loadColumn(tableName, columnName));
                    }
                }

                print(indicesOfSelectedRows, selectedColumns);
                return true;
            } catch (FileStorageException e) {
                System.out.println(e.getMessage());
                return false;
            }
        }

        private void print(List<Integer> indicesOfSelectedRows, List<Column> selectedColumns) {
            int countCols = selectedColumns.size();
            for (Integer indicesOfSelectedRow : indicesOfSelectedRows) {
                for (int j = 0; j < countCols; j++) {
                    int rowIndex = indicesOfSelectedRow;
                    System.out.print(selectedColumns.get(j).getData().get(rowIndex) + "    ");
                }
                System.out.println();
            }
        }
    }
}
