package FileWork.JSON;

import Exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileStorageTest {

    private JsonFileStorage storage;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storage = new JsonFileStorage();
    }

    @Test
    void testWriteAndReadFile() throws FileStorageException {
        Path filePath = tempDir.resolve("test.json");
        Map<String, String> data = Map.of("key", "value");

        storage.writeFile(filePath.toString(), data);
        Map<?, ?> result = storage.readFile(filePath.toString(), Map.class);

        assertTrue(storage.exists(filePath.toString()));
        assertEquals("value", result.get("key"));
    }

    @Test
    void shouldThrowNoFileException() {
        String path = tempDir.resolve("non_existent.json").toString();
        assertThrows(NoFileException.class, () -> storage.readFile(path, Map.class));
    }

    @Test
    void testDirectoryOperations() throws FileStorageException {
        Path subDir = tempDir.resolve("parent/child");
        String path = subDir.toString();

        storage.createDirectory(path);
        assertTrue(new File(path).exists());

        assertThrows(AlreadyExistsException.class, () -> storage.createDirectory(path));

        storage.deleteDirectory(tempDir.resolve("parent").toString());
        assertFalse(new File(path).exists());
    }

    @Test
    void testRenameFile() throws FileStorageException {
        Path original = tempDir.resolve("old.json");
        Path target = tempDir.resolve("new.json");

        storage.writeFile(original.toString(), Map.of("id", 1));

        storage.renameFile(original.toString(), target.toString());
        assertFalse(original.toFile().exists());
        assertTrue(target.toFile().exists());

        storage.writeFile(original.toString(), Map.of("id", 2));
        assertThrows(AlreadyExistsException.class, () ->
                storage.renameFile(original.toString(), target.toString()));
    }

    @Test
    void testDeleteFile() throws FileStorageException {
        Path file = tempDir.resolve("to_delete.json");
        storage.writeFile(file.toString(), "data");

        storage.deleteFile(file.toString());

        assertFalse(file.toFile().exists());
    }
}