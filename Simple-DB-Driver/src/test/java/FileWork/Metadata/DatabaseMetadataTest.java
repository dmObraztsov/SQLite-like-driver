package FileWork.Metadata;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseMetadataTest {

    @Test
    void testDateStorage() {
        Date now = new Date();
        DatabaseMetadata db = new DatabaseMetadata();

        db.setCreatedDate(now);
        db.setLastModified(now);

        assertEquals(now, db.getCreatedDate());
        assertEquals(now, db.getLastModified());
    }

    @Test
    void testFullConstructor() {
        Date created = new Date();
        DatabaseMetadata db = new DatabaseMetadata(
                "test_db", "1.0", "UTF-8", "utf8_general_ci", created, created
        );

        assertEquals("test_db", db.getName());
        assertEquals("UTF-8", db.getEncoding());
        assertEquals(created, db.getCreatedDate());
    }
}