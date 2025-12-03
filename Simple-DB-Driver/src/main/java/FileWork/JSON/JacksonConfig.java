package FileWork.JSON;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

public class JacksonConfig {

    public static ObjectMapper createConfiguredMapper() {
        ObjectMapper mapper = new ObjectMapper();

//        // Игнорировать пустые поля
//        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
//
//        // Красивое форматирование JSON
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
//
//        // Игнорировать неизвестные свойства
//        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//
//        // Не вызывать геттеры/сеттеры, если поля публичные
//        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
//        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
//        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);

        return mapper;
    }
}