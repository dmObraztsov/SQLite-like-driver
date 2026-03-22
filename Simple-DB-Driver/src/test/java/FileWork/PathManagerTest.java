package FileWork;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathManagerTest {

    @Test
    void shouldBuildDatabaseAndMetadataPaths() {
        assertThat(PathManager.getDatabasePath("shop")).isEqualTo("src/main/data/shop");
        assertThat(PathManager.getDatabaseMetadataPath("shop")).isEqualTo("src/main/data/shop/metadata.json");
    }

    @Test
    void shouldBuildTableAndColumnPaths() {
        assertThat(PathManager.getTablePath("shop", "users"))
                .isEqualTo("src/main/data/shop/tables/users");
        assertThat(PathManager.getTableMetadataPath("shop", "users"))
                .isEqualTo("src/main/data/shop/tables/users/metadata.json");
        assertThat(PathManager.getColumnPath("shop", "users", "id"))
                .isEqualTo("src/main/data/shop/tables/users/data/id.json");
        assertThat(PathManager.getColumnMetadataPath("shop", "users", "id"))
                .isEqualTo("src/main/data/shop/tables/users/data/id.metadata.json");
    }

    @Test
    void tableDataPathShouldContainTrailingSlash() {
        assertThat(PathManager.getTableDataPath("shop", "users"))
                .isEqualTo("src/main/data/shop/tables/users/data/");
    }

    @Test
    void shouldPreserveRawNamesWithoutSanitization() {
        assertThat(PathManager.getDatabasePath("my db"))
                .isEqualTo("src/main/data/my db");
        assertThat(PathManager.getTablePath("my db", "users-v2"))
                .isEqualTo("src/main/data/my db/tables/users-v2");
    }

    @AfterEach
    void tearDown() {
        PathManager.reset();
    }
}
