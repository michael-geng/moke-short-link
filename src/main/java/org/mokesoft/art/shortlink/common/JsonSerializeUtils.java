package org.mokesoft.art.shortlink.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializeUtils {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static String toString(Object object){

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
