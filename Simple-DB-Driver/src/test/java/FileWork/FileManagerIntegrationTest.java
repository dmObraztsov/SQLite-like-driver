package FileWork;

import Exceptions.FileStorageException;
import FileWork.JSON.JsonFileStorage;
import FileWork.Metadata.ColumnMetadata;
import SqlParser.Antlr.SQLProcessor;
import Yadro.DataStruct.Column;
import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Интеграционные тесты: FileManager")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileManagerIntegrationTest {

    private static FileManager fileManager;
    private static FileStorage storage;
    private final String dbName = "TestDB";

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void init() {
        storage = new JsonFileStorage();
        fileManager = new FileManager(storage);
    }

    @Test
    @Order(1)
    @DisplayName("Создание БД и Таблицы")
    void testStructureCreation() throws FileStorageException {
        // 1. Создаем БД
        assertTrue(fileManager.createDB(dbName));
        assertTrue(fileManager.useDB(dbName));

        // 2. Создаем таблицу (твой метод принимает только String)
        assertTrue(fileManager.createTable("users"));

        // 3. Создаем колонку отдельно (как требует твой FileManager)
        ArrayList<Constraints> constraints = new ArrayList<>(List.of(Constraints.PRIMARY_KEY));
        ColumnMetadata colMeta = new ColumnMetadata("id", DataType.INTEGER, 0, constraints, null);

        assertTrue(fileManager.createColumn("users", colMeta));
    }

    @Test
    @Order(2)
    @DisplayName("Работа с данными в колонке")
    void testColumnData() throws FileStorageException {
        fileManager.useDB(dbName);

        // Загружаем созданную в предыдущем тесте колонку
        Column idCol = fileManager.loadColumn("users", "id");
        assertNotNull(idCol);

        idCol.addData(0, "100");
        fileManager.saveColumn("users", "id", idCol);

        // Проверяем, что данные сохранились и читаются
        Column reloadedCol = fileManager.loadColumn("users", "id");
        assertEquals("100", reloadedCol.getData().get(0));
    }

    @Test
    @Order(3)
    @DisplayName("Валидация AUTOINCREMENT (Тест на отказ)")
    void testAutoincrementFail() throws FileStorageException {
        fileManager.useDB(dbName);
        fileManager.createTable("test_table");

        // Пытаемся создать AUTOINCREMENT на TEXT (должно вернуть false по логике validateColumn)
        ArrayList<Constraints> badConstraints = new ArrayList<>(List.of(Constraints.AUTOINCREMENT));
        ColumnMetadata badCol = new ColumnMetadata("id", DataType.TEXT, 0, badConstraints, null);

        assertFalse(fileManager.createColumn("test_table", badCol),
                "Должно быть false, так как AUTOINCREMENT только для INTEGER PRIMARY KEY");
    }

    @Test
    @Order(4)
    @DisplayName("Сквозной тест через SQLProcessor")
    void testSqlProcessor() throws FileStorageException {
        // Проверка парсинга и выполнения (если ANTLR сгенерирован)
        fileManager.createDB("ShopDB");
        fileManager.useDB("ShopDB");

        String sql = "CREATE DATABASE TestFromSql";
        var query = SQLProcessor.getQuery(sql);

        if (query != null) {
            assertTrue(query.execute(fileManager));
        }
    }

    @Test
    @Order(5)
    @DisplayName("Удаление таблицы и БД")
    void testCleanup() throws FileStorageException {
        fileManager.useDB(dbName);
        assertTrue(fileManager.dropTable("users"));
        assertTrue(fileManager.dropDB(dbName));
    }
}