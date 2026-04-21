package FileWork;

import Exceptions.EmptyFileException;
import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileManagerTest {

    @Mock
    FileStorage fileStorage;

    @Test
    void shouldThrowIfStorageIsNull() {
        assertThatThrownBy(() -> new FileManager(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void useDbShouldSetCurrentDbIfExists() {
        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);

        try {
            manager.useDB("testDb");
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }

        assertThat(manager.getNameDB()).isEqualTo("testDb");
    }

    @Test
    void useDbShouldNotChangeCurrentDbIfTargetDoesNotExist() throws FileStorageException {
        String existingDbPath = PathManager.getDatabasePath("db1");
        String missingDbPath = PathManager.getDatabasePath("missing");

        when(fileStorage.exists(existingDbPath)).thenReturn(true);
        when(fileStorage.exists(missingDbPath)).thenReturn(false);

        FileManager manager = new FileManager(fileStorage);

        manager.useDB("db1");
        assertEquals("db1", manager.getNameDB());

        assertThrows(EmptyFileException.class, () -> manager.useDB("missing"));

        assertEquals("db1", manager.getNameDB());
    }

    @Test
    void useDbShouldResetDbIfBlankName() {
        FileManager manager = new FileManager(fileStorage);
        try {
            manager.useDB("   ");
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }

        assertThat(manager.getNameDB()).isEmpty();
    }

    @Test
    void createDbShouldCreateDirectoryAndMetadata() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);

        manager.createDB("testDb");

        verify(fileStorage).createDirectory("src/main/data/testDb");
        verify(fileStorage).writeFile(eq("src/main/data/testDb/metadata.bin"), any());
    }

    @Test
    void dropDbShouldResetCurrentWhenDroppingCurrentDb() throws FileStorageException {
        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);
        manager.useDB("testDb");

        manager.dropDB("testDb");

        assertThat(manager.getNameDB()).isEmpty();
        verify(fileStorage).deleteDirectory("src/main/data/testDb");
    }

    @Test
    void dropDbShouldKeepCurrentWhenDroppingAnotherDb() throws FileStorageException {
        String expectedPath = PathManager.getDatabasePath("db1");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);
        manager.useDB("db1");

        manager.dropDB("db2");

        assertThat(manager.getNameDB()).isEqualTo("db1");
        verify(fileStorage).deleteDirectory("src/main/data/db2");
    }

    @Test
    void shouldSaveAndLoadTableMetadataViaStorage() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);
        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        manager.useDB("testDb");

        TableMetadata metadata = new TableMetadata();
        manager.saveTableMetadata("users", metadata);
        verify(fileStorage).writeFile("src/main/data/testDb/tables/users/metadata.bin", metadata);

        manager.loadTableMetadata("users");
        verify(fileStorage).readFile("src/main/data/testDb/tables/users/metadata.bin", TableMetadata.class);
    }

    @Test
    void createAndDropTableStructureShouldDelegateToStorage() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);

        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        manager.useDB("testDb");

        manager.createTableStructure("users");
        verify(fileStorage).createDirectory("src/main/data/testDb/tables/users");
        verify(fileStorage).createDirectory("src/main/data/testDb/tables/users/data/");

        manager.dropTableStructure("users");
        verify(fileStorage).deleteDirectory("src/main/data/testDb/tables/users");
    }

    @Test
    void shouldSaveAndLoadColumnDataAndMetadata() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        manager.useDB("testDb");

        Column column = new Column();
        ColumnMetadata metadata = new ColumnMetadata();

        manager.saveColumnData("users", "id", column);
        manager.saveColumnMetadata("users", "id", metadata);
        manager.loadColumnData("users", "id");
        manager.loadColumnMetadata("users", "id");

        verify(fileStorage).writeFile("src/main/data/testDb/tables/users/data/id.bin", column);
        verify(fileStorage).writeFile("src/main/data/testDb/tables/users/data/id.metadata.bin", metadata);
        verify(fileStorage).readFile("src/main/data/testDb/tables/users/data/id.bin", Column.class);
        verify(fileStorage).readFile("src/main/data/testDb/tables/users/data/id.metadata.bin", ColumnMetadata.class);
    }

    @Test
    void deleteColumnFilesShouldDeleteDataAndMetadataFiles() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);
        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        manager.useDB("testDb");

        manager.deleteColumnFiles("users", "id");

        verify(fileStorage).deleteFile("src/main/data/testDb/tables/users/data/id.bin");
        verify(fileStorage).deleteFile("src/main/data/testDb/tables/users/data/id.metadata.bin");
    }

    @Test
    void tableExistsShouldDelegateToStorage() {
        String expectedPath = PathManager.getDatabasePath("testDb");
        when(fileStorage.exists(expectedPath)).thenReturn(true);
        when(fileStorage.exists("src/main/data/testDb/tables/users")).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);
        try {
            manager.useDB("testDb");
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }

        boolean exists = manager.tableExists("users");

        assertThat(exists).isTrue();
    }

    @Test
    void renameOperationsShouldDelegateToStorage() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);

        manager.renameFile("a.bin", "b.bin");
        manager.renameDirectory("db1", "db2");

        verify(fileStorage).renameFile("a.bin", "b.bin");
        verify(fileStorage).renameDirectory(anyString(), anyString());
    }

    @AfterEach
    void tearDown() {
        PathManager.reset();
    }
}
