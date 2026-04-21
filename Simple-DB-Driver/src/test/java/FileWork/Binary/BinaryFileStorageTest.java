package FileWork.Binary;

import Exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BinaryFileStorageTest {

    @TempDir
    Path tempDir;

    private BinaryFileStorage storage;
    private String testFilePath;

    static class TestData implements Serializable {
        String name;
        int value;
        TestData(String name, int value) { this.name = name; this.value = value; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestData testData)) return false;
            return value == testData.value && name.equals(testData.name);
        }
    }

    @BeforeEach
    void setUp() {
        storage = new BinaryFileStorage(tempDir.toString());
        testFilePath = tempDir.resolve("test.bin").toString();
    }

    @Test
    void testWriteAndReadObject() throws FileStorageException {
        TestData original = new TestData("Yadro", 2026);
        storage.writeFile(testFilePath, original);

        TestData restored = storage.readFile(testFilePath, TestData.class);
        assertEquals(original, restored);
    }

    @Test
    void testWriteAndReadList() throws FileStorageException {
        List<String> list = List.of("one", "two", "three");
        storage.writeFile(testFilePath, new ArrayList<>(list));

        List<?> restored = storage.readFile(testFilePath, ArrayList.class);
        assertEquals(list, restored);
    }

    @Test
    void testReadNonExistentFileThrowsException() {
        assertThrows(NoFileException.class, () ->
                storage.readFile(tempDir.resolve("ghost.bin").toString(), String.class)
        );
    }

    @Test
    void testReadEmptyFileThrowsException() throws IOException {
        File emptyFile = new File(testFilePath);
        emptyFile.createNewFile(); // 0 byte

        assertThrows(EmptyFileException.class, () ->
                storage.readFile(testFilePath, String.class)
        );
    }

    @Test
    void testDeleteFile() throws FileStorageException {
        storage.writeFile(testFilePath, "delete me");
        assertTrue(storage.exists(testFilePath));

        storage.deleteFile(testFilePath);
        assertFalse(storage.exists(testFilePath));
    }

    @Test
    void testRenameFile() throws FileStorageException {
        String newPath = tempDir.resolve("renamed.bin").toString();
        storage.writeFile(testFilePath, "content");

        storage.renameFile(testFilePath, newPath);

        assertFalse(storage.exists(testFilePath));
        assertTrue(storage.exists(newPath));
    }

    @Test
    void testDirectoryOperations() throws FileStorageException {
        String dirPath = tempDir.resolve("subfolder").toString();

        storage.createDirectory(dirPath);
        assertTrue(new File(dirPath).isDirectory());

        assertThrows(AlreadyExistsException.class, () -> storage.createDirectory(dirPath));

        storage.deleteDirectory(dirPath);
        assertFalse(new File(dirPath).exists());
    }

    @Test
    void testRecursiveDeleteDirectory() throws FileStorageException {
        String parentDir = tempDir.resolve("parent").toString();
        String childFile = tempDir.resolve("parent/child.bin").toString();

        storage.createDirectory(parentDir);
        storage.writeFile(childFile, "inside");

        storage.deleteDirectory(parentDir);
        assertFalse(new File(parentDir).exists());
    }
}