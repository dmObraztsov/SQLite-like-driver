package SqlParser.QueriesStruct;

import Exceptions.*;
import FileWork.FileManager;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import SqlParser.Antlr.SQLParser;
import Yadro.DataStruct.*;
import java.util.ArrayList;
import java.util.List;

public class Queries {

    public static class SelectDataQuery implements QueryInterface {
        private List<String>                       selectCols;
        private final boolean                      isStar;
        private final String                       tableName;
        private final SQLParser.WhereClauseContext whereClause;

        public SelectDataQuery(List<String> selectCols, Boolean isStar, String tableName, SQLParser.WhereClauseContext whereClause) {
            this.selectCols = selectCols;
            this.tableName = tableName;
            this.whereClause = whereClause;
            this.isStar = isStar;
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException{
            //Грузим метадату нашей таблицы
            TableMetadata tableMetadata              =   fileManager.loadTableMetadata(tableName);
            List<String>  tableMetadataColumnNames   =   tableMetadata.getColumnNames();
            int           tableMetadataColumnCount   =   tableMetadataColumnNames.size();

            if(tableMetadataColumnCount == 0) {//Если у нас в таблице нет вообще ничего
                //TODO Тут какой нить эксепшн
                return false;
            }
            List<Integer> indicesOfSelectedRows = new ArrayList<>();
            List<Column> columns = new ArrayList<>();
            List<Column> selectedColumns = new ArrayList<>();

            //Загружаем все столбцы из таблицы
            for(String columnName : tableMetadataColumnNames) {
                //TODO Тут используется файл менеджер, так что САФОНОВ ОПЛАТИТЬ(Соня сделать эксепшн)
                columns.add(fileManager.loadColumn(tableName, columnName));
            }
            int columnSize = columns.getFirst().getData().size();

            if(isStar) {
                selectedColumns.addAll(columns);
                for(int i = 0; i < columnSize; i++) {
                    indicesOfSelectedRows.add(i);
                }
            }
            else {
                //Проверяем есть ли каждый выбранный столбец в нашей таблице
                for(String columnName : selectCols) {
                    if(!tableMetadataColumnNames.contains(columnName)) {
                        //TODO Соня допилить эксепшн
                        return false;
                    }
                }

                //Обрабатываем WHERE если есть
                if (whereClause != null) {
                    String whereName = whereClause.name().getText();
                    String whereValue = whereClause.value().getText();

                    //TODO Тут используется файл менеджер, так что САФОНОВ ОПЛАТИТЬ(Соня сделать эксепшн)
                    Column whereColumn = fileManager.loadColumn(tableName, whereName);

                    ArrayList<String> whereColumnData = whereColumn.getData();
                    //Пробегаем по столбцу whereColumn и добавляем индексы строк, которые удовлетворяют условиям WHERE
                    for(int i = 0; i < columnSize; i++) {
                        if(whereColumnData.get(i).equals(whereValue)) {
                            indicesOfSelectedRows.add(i);
                        }
                    }
                }
                else { //Если WHERE нет, то indicesOfSelectedRows будет просто содержать индексы всех строк
                    for(int i = 0; i < columnSize; i++) {
                        indicesOfSelectedRows.add(i);
                    }
                }

                for(String columnName : selectCols) {
                    selectedColumns.add(fileManager.loadColumn(tableName, columnName));
                }
            }

            //TODO вообще конечно в идеале делать красивый вывод. Я имею ввиду нормальное выравнивание колонок
            int countRows = indicesOfSelectedRows.size();
            int countCols = selectedColumns.size();
            for(int i = 0; i < countRows; i++) {
                for(int j = 0; j < countCols; j++) {
                    int rowIndex = indicesOfSelectedRows.get(i);
                    System.out.print(selectedColumns.get(j).getData().get(rowIndex) + "    ");
                }
                System.out.println();
            }

            return true;
        }


        //TODO Допилить здесь нормальное строковое представление
        @Override
        public String getStringVision(){
            return "SELECT";
        }

    }

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

        @Override
        public String getStringVision() {
            return "Creating database with mame " + "\"" + databaseName + "\"";
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

        @Override
        public String getStringVision() {
            return "Drop database with mame " + "\"" + databaseName + "\"";
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

        @Override
        public String getStringVision() {
            return "Use database with mame " + "\"" + databaseName + "\"";
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

        @Override
        public String getStringVision() {
            return "Creating table with mame " + "\"" + tableName + "\"";
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

        @Override
        public String getStringVision() {
            return "Drop table with mame " + "\"" + tableName + "\"";
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

        @Override
        public String getStringVision() {
            return "Alter table with mame " + "\"" + tableName + "\"";
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

            @Override
            public String getStringVision() {
                return "Rename table with mame " + "\"" + super.tableName + "to " + this.changeTableName + "\"";
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

            @Override
            public String getStringVision() {
                return "Add column to table with mame " + "\"" + super.tableName + "\"";
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

            @Override
            public String getStringVision() {
                return "Add column to table with mame " + "\"" + super.tableName + "\"";
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

            @Override
            public String getStringVision() {
                return "Add column to table with mame " + "\"" + super.tableName + "\"";
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

        public record ColumnValue(String value, String column) {
        }

        @Override
        public boolean execute(FileManager fileManager) throws FileStorageException {
            //TODO пока вставляю все колонки, но потом нужно использовать только те что из запроса и понимать
            // какие можно вставить NULL, а какие нельзя и бросить исключение если не сходится кол-во
            TableMetadata tableMetadata = fileManager.loadTableMetadata(tableName);
            columns = new ArrayList<>(tableMetadata.getColumnNames());

            ArrayList<ColumnValue> checkedRowToAdd = new ArrayList<>();
            int j = 0;

            for(String curr : columns) {
                Column column = fileManager.loadColumn(tableName, curr);
                ColumnMetadata columnMetadata = fileManager.loadColumnMetadata(tableName, curr);

                String value;

                value = values.get(j++);

                if(fullCheck(column, columnMetadata, value)) {
                    checkedRowToAdd.add(new ColumnValue(value, curr));

                }
            }

            return IndexWorker.AddToDataBase(checkedRowToAdd, fileManager, tableName);
        }

        @Override
        public String getStringVision() {
            return "";
        }

        private boolean fullCheck(Column column, ColumnMetadata columnMetadata, String content) {
            return (providedType(content) == columnMetadata.getType()) && checkConstraints(column, columnMetadata, content);
        }

        private DataType providedType(String content) {
            if(content.equals("NULL")) return DataType.NULL;
            if(content.matches("-?\\d+")) return DataType.INTEGER;
            if(content.matches("-?\\d+(\\.\\d+)?")) return DataType.REAL;
            if(content.startsWith("\"") && content.endsWith("\"") && content.length() >= 2) return DataType.TEXT;
            else return null;
        }

        private boolean checkConstraints(Column column, ColumnMetadata columnMetadata, String content) {
            for(Constraints curr : columnMetadata.getConstraints()) {
                switch (curr) {
                    case UNIQUE:
                        if(column.getData().contains(content)) return false;
                        break;
                    case NOT_NULL:
                        if(content == null) return false;
                        break;
                    case CHECK:                        //TODO
                        break;
                }
            }
            return true;
        }
    }
}
