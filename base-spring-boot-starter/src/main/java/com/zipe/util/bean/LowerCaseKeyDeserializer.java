package com.zipe.util.bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:49
 **/
public class LowerCaseKeyDeserializer extends KeyDeserializer {
    public LowerCaseKeyDeserializer() {
    }

    public Object deserializeKey(String key, DeserializationContext ctx) throws IOException, JsonProcessingException {
        return lowerCaseFirst(key);
    }

    private static String lowerCaseFirst(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        } else {
            char[] array = value.toCharArray();
            array[0] = Character.toLowerCase(array[0]);
            return new String(array);
        }
    }
}
