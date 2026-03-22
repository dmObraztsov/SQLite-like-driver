package FileWork.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigTest {

    @Test
    void shouldCreateConfiguredMapper() {
        ObjectMapper mapper = JacksonConfig.createConfiguredMapper();

        assertNotNull(mapper);
        assertTrue(mapper.getSerializationConfig().isEnabled(SerializationFeature.INDENT_OUTPUT));
    }

    @Test
    void shouldSerializePrivateFieldsDirectly() throws Exception {
        ObjectMapper mapper = JacksonConfig.createConfiguredMapper();

        // Внутренний тестовый класс
        var obj = new Object() {
            private String secret = "hidden";
        };

        String json = mapper.writeValueAsString(obj);
        assertTrue(json.contains("secret"), "Поле 'secret' должно быть в JSON, даже если оно приватное");
    }
}