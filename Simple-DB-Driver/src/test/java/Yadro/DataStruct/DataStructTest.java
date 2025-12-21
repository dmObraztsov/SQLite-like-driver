package Yadro.DataStruct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Юнит-тесты: Структуры данных Yadro")
class DataStructTest {

    @Test
    @DisplayName("Проверка DataType: Соответствие SQL типов и Java классов")
    void testDataTypeMapping() {
        assertAll(
                () -> assertEquals(Long.class, DataType.INTEGER.getJavaType(), "INTEGER должен быть Long"),
                () -> assertEquals(Double.class, DataType.REAL.getJavaType(), "REAL должен быть Double"),
                () -> assertEquals(String.class, DataType.TEXT.getJavaType(), "TEXT должен быть String"),
                () -> assertEquals(Void.class, DataType.NULL.getJavaType(), "NULL должен быть Void")
        );
    }

    @Test
    @DisplayName("Проверка DataType: Дефолтные размеры")
    void testDataTypeSizes() {
        assertEquals(8, DataType.INTEGER.getDefaultSize());
        assertEquals(255, DataType.TEXT.getDefaultSize());
    }

    @Test
    @DisplayName("Проверка Column: Добавление данных по индексу")
    void testColumnAddData() {
        Column column = new Column();
        column.addData(0, "First Value");
        column.addData(1, "Second Value");

        ArrayList<String> data = column.getData();
        assertEquals(2, data.size());
        assertEquals("First Value", data.get(0));
        assertEquals("Second Value", data.get(1));
    }

    @Test
    @DisplayName("Проверка перечислений Constraints и Collate")
    void testEnums() {
        // Проверяем наличие ключевых констант
        assertNotNull(Constraints.valueOf("PRIMARY_KEY"));
        assertNotNull(Constraints.valueOf("NOT_NULL"));
        assertNotNull(Collate.valueOf("NOCASE"));
    }
}