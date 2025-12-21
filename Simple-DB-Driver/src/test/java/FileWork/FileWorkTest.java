package FileWork;

import Exceptions.EmptyFileException;
import Exceptions.FileStorageException;
import Exceptions.NoFileException;
import FileWork.JSON.JsonFileStorage;
import FileWork.Metadata.DatabaseMetadata;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Юнит-тесты: Работа с файлами и JSON")
class FileWorkTest {

    private JsonFileStorage storage;
    private FileManager fileManager;

    @TempDir
    Path tempDir; // Создает временную папку для тестов

    @BeforeEach
    void setUp() {
        storage = new JsonFileStorage();
        // Инициализируем FileManager, чтобы ошибка 'Cannot resolve symbol' исчезла
        fileManager = new FileManager(storage);
    }

    @Test
    @DisplayName("Проверка PathManager: Генерация путей")
    void testPathManager() {
        String dbName = "testDB";
        String expectedPath = "src/main/data/testDB/metadata.json";
        // Заменяем слеши на системные, чтобы тест проходил на разных ОС
        assertEquals(expectedPath.replace("/", File.separator),
                PathManager.getDatabaseMetadataPath(dbName).replace("/", File.separator));
    }

    @Test
    @DisplayName("Проверка JsonFileStorage: Создание и удаление директории")
    void testDirectoryOperations() throws FileStorageException {
        String dirPath = tempDir.resolve("new_folder").toString();

        storage.createDirectory(dirPath);
        assertTrue(new File(dirPath).exists());
        assertTrue(new File(dirPath).isDirectory());

        storage.deleteDirectory(dirPath);
        assertFalse(new File(dirPath).exists());
    }

    @Test
    @DisplayName("Проверка JsonFileStorage: Запись и чтение метаданных БД")
    void testWriteAndReadMetadata() throws FileStorageException {
        String filePath = tempDir.resolve("metadata.json").toString();
        DatabaseMetadata originalMetadata = new DatabaseMetadata(
                "TestDB", "1.0", "UTF-8", "BINARY", new Date(), new Date()
        );

        // Тест записи
        storage.writeFile(filePath, originalMetadata);
        assertTrue(new File(filePath).exists());

        // Тест чтения и десериализации
        DatabaseMetadata loadedMetadata = storage.readFile(filePath, DatabaseMetadata.class);
        assertEquals(originalMetadata.getName(), loadedMetadata.getName());
        assertEquals(originalMetadata.getEncoding(), loadedMetadata.getEncoding());
    }

    @Test
    @DisplayName("Проверка обработки исключений: Файл не найден")
    void testNoFileException() {
        String nonExistentPath = tempDir.resolve("missing.json").toString();
        assertThrows(NoFileException.class, () -> storage.readFile(nonExistentPath, DatabaseMetadata.class));
    }

    @Test
    @DisplayName("Проверка удаления непустой директории (рекурсивно)")
    void testRecursiveDelete() throws FileStorageException {
        Path subDir = tempDir.resolve("parent");
        Path fileInDir = subDir.resolve("child.json");

        storage.createDirectory(subDir.toString());
        // Создаем пустой объект для записи
        storage.writeFile(fileInDir.toString(), new DatabaseMetadata());

        // Должно удалить папку вместе с файлом
        storage.deleteDirectory(subDir.toString());
        assertFalse(subDir.toFile().exists());
    }

    @Test
    @DisplayName("Тестирование на отказ: Пустой файл метаданных")
    void testEmptyMetadataFile() throws Exception {
        // Используем временную директорию для теста, чтобы не плодить файлы в проекте
        String dbName = "EmptyDB";
        String dbPath = tempDir.resolve(dbName).toString();
        String metadataPath = tempDir.resolve("empty_metadata.json").toString();

        // Создаем пустой файл вручную (имитация повреждения)
        java.nio.file.Files.write(java.nio.file.Paths.get(metadataPath), new byte[0]);

        // Проверяем, что JsonFileStorage выбросит EmptyFileException при чтении
        assertThrows(EmptyFileException.class, () -> {
            storage.readFile(metadataPath, DatabaseMetadata.class);
        });
    }
}