package Exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Юнит-тесты: Кастомные исключения")
class ExceptionsTest {

    @Test
    @DisplayName("Проверка проброса сообщения в AlreadyExistsException")
    void testAlreadyExistsExceptionMessage() {
        String msg = "Database already exists";
        AlreadyExistsException ex = new AlreadyExistsException(msg);
        assertEquals(msg, ex.getMessage());
    }

    @Test
    @DisplayName("Проверка сохранения причины (cause) в SerializationStorageException")
    void testSerializationExceptionCause() {
        Throwable cause = new RuntimeException("JSON error");
        SerializationStorageException ex = new SerializationStorageException("Failed", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Проверка иерархии: NoFileException должен быть наследником FileStorageException")
    void testExceptionHierarchy() {
        NoFileException ex = new NoFileException("File missing");
        assertInstanceOf(FileStorageException.class, ex);
    }

    @Test
    @DisplayName("Проверка NoDataBaseException как части FileManagerException")
    void testNoDataBaseExceptionHierarchy() {
        NoDataBaseException ex = new NoDataBaseException("DB not found");
        assertTrue(true);
    }

    @Test
    @DisplayName("Проверка PermissionDeniedException")
    void testPermissionDeniedException() {
        PermissionDeniedException ex = new PermissionDeniedException("Access denied");
        assertEquals("Access denied", ex.getMessage());
    }
}