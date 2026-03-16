package FileWork;

import Exceptions.FileStorageException;
import FileWork.Metadata.ColumnMetadata;
import FileWork.Metadata.TableMetadata;
import Yadro.DataStruct.Column;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);

        manager.useDB("testDb");

        assertThat(manager.getNameDB()).isEqualTo("testDb");
    }

    @Test
    void useDbShouldNotChangeCurrentDbIfTargetDoesNotExist() {
        when(fileStorage.exists("src/main/data/db1")).thenReturn(true);
        when(fileStorage.exists("src/main/data/missing")).thenReturn(false);
        FileManager manager = new FileManager(fileStorage);

        manager.useDB("db1");
        manager.useDB("missing");

        assertThat(manager.getNameDB()).isEqualTo("db1");
    }

    @Test
    void useDbShouldResetDbIfBlankName() {
        FileManager manager = new FileManager(fileStorage);
        manager.useDB("   ");

        assertThat(manager.getNameDB()).isEmpty();
    }

    @Test
    void createDbShouldCreateDirectoryAndMetadata() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);

        manager.createDB("testDb");

        verify(fileStorage).createDirectory("src/main/data/testDb");
        verify(fileStorage).writeFile(eq("src/main/data/testDb/metadata.json"), any());
    }

    @Test
    void dropDbShouldResetCurrentWhenDroppingCurrentDb() throws FileStorageException {
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);
        manager.useDB("testDb");

        manager.dropDB("testDb");

        assertThat(manager.getNameDB()).isEmpty();
        verify(fileStorage).deleteDirectory("src/main/data/testDb");
    }

    @Test
    void dropDbShouldKeepCurrentWhenDroppingAnotherDb() throws FileStorageException {
        when(fileStorage.exists("src/main/data/db1")).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);
        manager.useDB("db1");

        manager.dropDB("db2");

        assertThat(manager.getNameDB()).isEqualTo("db1");
        verify(fileStorage).deleteDirectory("src/main/data/db2");
    }

    @Test
    void shouldSaveAndLoadTableMetadataViaStorage() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
        manager.useDB("testDb");

        TableMetadata metadata = new TableMetadata();
        manager.saveTableMetadata("users", metadata);
        verify(fileStorage).writeFile("src/main/data/testDb/tables/users/metadata.json", metadata);

        manager.loadTableMetadata("users");
        verify(fileStorage).readFile("src/main/data/testDb/tables/users/metadata.json", TableMetadata.class);
    }

    @Test
    void createAndDropTableStructureShouldDelegateToStorage() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
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
        manager.useDB("testDb");

        Column column = new Column();
        ColumnMetadata metadata = new ColumnMetadata();

        manager.saveColumnData("users", "id", column);
        manager.saveColumnMetadata("users", "id", metadata);
        manager.loadColumnData("users", "id");
        manager.loadColumnMetadata("users", "id");

        verify(fileStorage).writeFile("src/main/data/testDb/tables/users/data/id.json", column);
        verify(fileStorage).writeFile("src/main/data/testDb/tables/users/data/id.metadata.json", metadata);
        verify(fileStorage).readFile("src/main/data/testDb/tables/users/data/id.json", Column.class);
        verify(fileStorage).readFile("src/main/data/testDb/tables/users/data/id.metadata.json", ColumnMetadata.class);
    }

    @Test
    void deleteColumnFilesShouldDeleteDataAndMetadataFiles() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
        manager.useDB("testDb");

        manager.deleteColumnFiles("users", "id");

        verify(fileStorage).deleteFile("src/main/data/testDb/tables/users/data/id.json");
        verify(fileStorage).deleteFile("src/main/data/testDb/tables/users/data/id.metadata.json");
    }

    @Test
    void tableExistsShouldDelegateToStorage() {
        when(fileStorage.exists("src/main/data/testDb")).thenReturn(true);
        when(fileStorage.exists("src/main/data/testDb/tables/users")).thenReturn(true);
        FileManager manager = new FileManager(fileStorage);
        manager.useDB("testDb");

        boolean exists = manager.tableExists("users");

        assertThat(exists).isTrue();
    }

    @Test
    void renameOperationsShouldDelegateToStorage() throws FileStorageException {
        FileManager manager = new FileManager(fileStorage);

        manager.renameFile("a.json", "b.json");
        manager.renameDirectory("db1", "db2");

        verify(fileStorage).renameFile("a.json", "b.json");
        verify(fileStorage).renameDirectory("db1", "db2");
    }
}
