package Exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionsTest {

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                Arguments.of(AlreadyExistsException.class),
                Arguments.of(EmptyFileException.class),
                Arguments.of(FileManagerException.class),
                Arguments.of(FileStorageException.class),
                Arguments.of(FileTypeException.class),
                Arguments.of(NoDataBaseException.class),
                Arguments.of(NoFileException.class),
                Arguments.of(NoTableException.class),
                Arguments.of(PermissionDeniedException.class),
                Arguments.of(SerializationStorageException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void shouldStoreMessage(Class<? extends Exception> exceptionClass) throws Exception {
        String message = "Custom error occurred";

        Exception exception = exceptionClass.getConstructor(String.class).newInstance(message);

        assertEquals(message, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void shouldStoreCause(Class<? extends Exception> exceptionClass) throws Exception {
        String message = "Main error";
        Throwable cause = new RuntimeException("Original trigger");

        Exception exception = exceptionClass.getConstructor(String.class, Throwable.class)
                .newInstance(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        NoTableException ex = new NoTableException("table fail");

        assertInstanceOf(FileStorageException.class, ex);
        assertInstanceOf(Exception.class, ex);
    }
}