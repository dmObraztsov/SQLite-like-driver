package FileWork.Metadata;

import Yadro.DataStruct.Constraints;
import Yadro.DataStruct.DataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ColumnMetadataTest {

    @Test
    void testIncrementSize() {
        ColumnMetadata metadata = new ColumnMetadata();
        metadata.setSize(10);

        metadata.incrementSize();

        assertEquals(11, metadata.getSize());
    }

    @Test
    void testFullConstructor() {
        ArrayList<Constraints> constraints = new ArrayList<>();
        constraints.add(Constraints.PRIMARY_KEY);

        ColumnMetadata metadata = new ColumnMetadata("id", DataType.INTEGER, 1, constraints, null);

        assertEquals("id", metadata.getName());
        assertEquals(DataType.INTEGER, metadata.getType());
        assertTrue(metadata.getConstraints().contains(Constraints.PRIMARY_KEY));
    }
}