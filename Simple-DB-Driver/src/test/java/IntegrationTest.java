import FileWork.FileManager;
import FileWork.FileStorage;
import FileWork.JSON.JsonFileStorage;
import FileWork.PathManager;
import SqlParser.Antlr.SQLProcessor;
import Yadro.DataStruct.DatabaseEngine;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private DatabaseEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        String TEST_DB_DIR = "test_e2e_db";
        deleteDirectory(new File(TEST_DB_DIR));
        PathManager.setBasePath(TEST_DB_DIR);
        FileStorage storage = new JsonFileStorage(TEST_DB_DIR);
        FileManager fileManager = new FileManager(storage);
        engine = new DatabaseEngine(fileManager);

        engine.createDatabase("test_db");
        engine.setCurrentDatabase("test_db");
    }

    @Test
    void shouldHandleFullSqlCycle() throws Exception {
        execute("CREATE TABLE users (id INTEGER, name TEXT)");
        execute("CREATE TABLE orders (id INTEGER, u_id INTEGER, item TEXT)");

        execute("INSERT INTO users VALUES (1, \"Ivan\")");
        execute("INSERT INTO users VALUES (2, \"Petr\")");
        execute("INSERT INTO orders VALUES (101, 1, \"Laptop\")");
        execute("INSERT INTO orders VALUES (102, 1, \"Mouse\")");

        String sql = "SELECT DISTINCT users.name FROM users JOIN orders ON users.id = orders.u_id";

        var result = SQLProcessor.getQuery(sql).execute(engine);
        assertNotNull(result, "Результат выполнения не должен быть null");

        assertFalse(result.getRows().isEmpty(), "Список строк не должен быть пустым");
        System.out.println("FULL ROW DATA: " + result.getRows().get(0).values());
        assertEquals("Ivan", result.getRows().get(0).get("name"));
    }

    private void execute(String sql) throws Exception {
        var query = SQLProcessor.getQuery(sql);
        if (query != null) {
            query.execute(engine);
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            try (var stream = Files.walk(directory.toPath())) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                System.err.println("Не удалось очистить папку: " + e.getMessage());
            }
        }
    }

    @AfterEach
    void tearDown() {
        PathManager.reset();
    }
}