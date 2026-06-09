package com.zipe.util.bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 將 JSON 物件的鍵（key）首字母轉為小寫的自訂反序列化器。
 *
 * <p>繼承 Jackson 的 {@link KeyDeserializer}，可搭配
 * {@code @JsonDeserialize(keyUsing = LowerCaseKeyDeserializer.class)} 使用，
 * 確保反序列化後的 {@link java.util.Map} 鍵統一以小駝峰（lower camel case）開頭，
 * 避免因 JSON 鍵首字母大小寫不一致而導致的取值失敗。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:49
 **/
public class LowerCaseKeyDeserializer extends KeyDeserializer {

    /**
     * 建立 LowerCaseKeyDeserializer 實例。
     */
    public LowerCaseKeyDeserializer() {
    }

    /**
     * 將 JSON 鍵的首字母轉為小寫後回傳，作為 Map 的鍵使用。
     *
     * @param key 原始 JSON 鍵字串
     * @param ctx Jackson 反序列化上下文
     * @return 首字母轉小寫後的鍵字串；若原始鍵為空白則回傳空字串
     * @throws IOException              當底層 I/O 發生錯誤時拋出
     * @throws JsonProcessingException  當 JSON 處理發生錯誤時拋出
     */
    public Object deserializeKey(String key, DeserializationContext ctx) throws IOException, JsonProcessingException {
        return lowerCaseFirst(key);
    }

    /**
     * 將字串的第一個字元轉為小寫。
     *
     * @param value 原始字串
     * @return 首字母小寫後的字串；若輸入為空白則回傳空字串
     */
    private static String lowerCaseFirst(String value) {
        // 空白字串直接回傳空字串，避免後續 char 陣列操作發生 IndexOutOfBoundsException
        if (StringUtils.isBlank(value)) {
            return "";
        } else {
            char[] array = value.toCharArray();
            // 僅將首字母轉小寫，其餘字元保持不變
            array[0] = Character.toLowerCase(array[0]);
            return new String(array);
        }
    }
}
