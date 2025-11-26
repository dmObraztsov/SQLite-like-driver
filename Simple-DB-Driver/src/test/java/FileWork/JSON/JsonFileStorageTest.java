package FileWork.JSON;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileStorageTest {

    @TempDir
    Path tempDir;

    private JsonFileStorage storage;
    private String testFilePath;
    private String testDirPath;

    @BeforeEach
    void setUp() {
        storage = new JsonFileStorage();
        testFilePath = tempDir.resolve("test.json").toString();
        testDirPath = tempDir.resolve("testDir").toString();
    }

    @Test
    @DisplayName("Should return true when file exists")
    void testExists_FileExists() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("test.json"));

        // When
        boolean result = storage.exists(testFilePath);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when file does not exist")
    void testExists_FileNotExists() {
        // When
        boolean result = storage.exists(testFilePath);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should successfully read JSON file")
    void testReadFile_Success() throws IOException {
        // Given
        TestData testData = new TestData("John", 30);
        storage.writeFile(testFilePath, testData);

        // When
        TestData result = storage.readFile(testFilePath, TestData.class);

        // Then
        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
    }

    @Test
    @DisplayName("Should return null when reading non-existent file")
    void testReadFile_FileNotExists() {
        // When
        TestData result = storage.readFile(testFilePath, TestData.class);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when reading empty file")
    void testReadFile_EmptyFile() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("empty.json"));
        String emptyFilePath = tempDir.resolve("empty.json").toString();

        // When
        TestData result = storage.readFile(emptyFilePath, TestData.class);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should successfully write JSON file")
    void testWriteFile_Success() {
        // Given
        TestData testData = new TestData("Alice", 25);

        // When
        boolean result = storage.writeFile(testFilePath, testData);

        // Then
        assertTrue(result);
        assertTrue(new File(testFilePath).exists());
    }

    @Test
    @DisplayName("Should return false when writing to invalid path")
    void testWriteFile_InvalidPath() {
        // Given
        TestData testData = new TestData("Bob", 35);
        String invalidPath = "/invalid/path/test.json";

        // When
        boolean result = storage.writeFile(invalidPath, testData);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should successfully delete file")
    void testDeleteFile_Success() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("test.json"));

        // When
        boolean result = storage.deleteFile(testFilePath);

        // Then
        assertTrue(result);
        assertFalse(new File(testFilePath).exists());
    }

    @Test
    @DisplayName("Should return false when deleting non-existent file")
    void testDeleteFile_FileNotExists() {
        // When
        boolean result = storage.deleteFile(testFilePath);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should successfully rename file")
    void testRenameFile_Success() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("test.json"));
        String newPath = tempDir.resolve("renamed.json").toString();

        // When
        boolean result = storage.renameFile(testFilePath, newPath);

        // Then
        assertTrue(result);
        assertFalse(new File(testFilePath).exists());
        assertTrue(new File(newPath).exists());
    }

    @Test
    @DisplayName("Should return false when renaming non-existent file")
    void testRenameFile_FileNotExists() {
        // Given
        String newPath = tempDir.resolve("renamed.json").toString();

        // When
        boolean result = storage.renameFile(testFilePath, newPath);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should successfully create directory")
    void testCreateDirectory_Success() {
        // When
        boolean result = storage.createDirectory(testDirPath);

        // Then
        assertTrue(result);
        assertTrue(new File(testDirPath).exists());
        assertTrue(new File(testDirPath).isDirectory());
    }

    @Test
    @DisplayName("Should return false when directory already exists")
    void testCreateDirectory_AlreadyExists() throws IOException {
        // Given
        Files.createDirectory(tempDir.resolve("testDir"));

        // When
        boolean result = storage.createDirectory(testDirPath);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should successfully delete empty directory")
    void testDeleteDirectory_Success() throws IOException {
        // Given
        Files.createDirectory(tempDir.resolve("testDir"));

        // When
        boolean result = storage.deleteDirectory(testDirPath);

        // Then
        assertTrue(result);
        assertFalse(new File(testDirPath).exists());
    }

    @Test
    @DisplayName("Should successfully delete directory with files")
    void testDeleteDirectory_WithFiles() throws IOException {
        // Given
        Path dirPath = tempDir.resolve("testDir");
        Files.createDirectory(dirPath);
        Files.createFile(dirPath.resolve("file1.txt"));
        Files.createFile(dirPath.resolve("file2.txt"));

        // When
        boolean result = storage.deleteDirectory(dirPath.toString());

        // Then
        assertTrue(result);
        assertFalse(Files.exists(dirPath));
    }

    @Test
    @DisplayName("Should return false when deleting non-existent directory")
    void testDeleteDirectory_NotExists() {
        // When
        boolean result = storage.deleteDirectory(testDirPath);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should successfully rename directory")
    void testRenameDirectory_Success() throws IOException {
        // Given
        Files.createDirectory(tempDir.resolve("testDir"));
        String newPath = tempDir.resolve("renamedDir").toString();

        // When
        boolean result = storage.renameDirectory(testDirPath, newPath);

        // Then
        assertTrue(result);
        assertFalse(new File(testDirPath).exists());
        assertTrue(new File(newPath).exists());
    }

    @Test
    @DisplayName("Should return false when renaming non-existent directory")
    void testRenameDirectory_NotExists() {
        // Given
        String newPath = tempDir.resolve("renamedDir").toString();

        // When
        boolean result = storage.renameDirectory(testDirPath, newPath);

        // Then
        assertFalse(result);
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle reading invalid JSON format")
        void testReadFile_InvalidJsonFormat() throws IOException {
            // Given
            String filePath = tempDir.resolve("invalid.json").toString();
            Files.writeString(Path.of(filePath), "invalid json content");

            // When
            Object result = storage.readFile(filePath, Object.class);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should handle complex nested directory structure")
        void testDeleteDirectory_NestedStructure() throws IOException {
            // Given
            Path rootDir = tempDir.resolve("root");
            Path subDir1 = rootDir.resolve("sub1");
            Path subDir2 = rootDir.resolve("sub2");
            Path file1 = subDir1.resolve("file1.txt");
            Path file2 = subDir2.resolve("file2.txt");

            Files.createDirectories(subDir1);
            Files.createDirectories(subDir2);
            Files.createFile(file1);
            Files.createFile(file2);

            // When
            boolean result = storage.deleteDirectory(rootDir.toString());

            // Then
            assertTrue(result);
            assertFalse(Files.exists(rootDir));
        }

        @Test
        @DisplayName("Should handle special characters in file names")
        void testWriteFile_WithSpecialCharacters() {
            // Given
            String specialPath = tempDir.resolve("test-file_123.json").toString();
            TestData testData = new TestData("Special", 99);

            // When
            boolean result = storage.writeFile(specialPath, testData);

            // Then
            assertTrue(result);
            assertTrue(new File(specialPath).exists());
        }
    }

    // Test data class for JSON serialization/deserialization
    static class TestData {
        private String name;
        private int age;

        public TestData() {
            // Default constructor for Jackson
        }

        public TestData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}