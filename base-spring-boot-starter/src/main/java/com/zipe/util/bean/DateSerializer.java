package com.zipe.util.bean;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 自訂 Jackson {@link Date} 序列化器，將 {@code java.util.Date} 物件格式化為
 * 固定格式的日期時間字串（{@code yyyy/MM/dd HH:mm:ss}）後輸出至 JSON。
 * <p>
 * 可搭配 {@code @JsonSerialize(using = DateSerializer.class)} 或全域 Jackson
 * ObjectMapper 設定，統一 API 回應中的日期格式。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 下午 01:31
 **/
public class DateSerializer extends StdSerializer<Date> {
    private static final long serialVersionUID = 1L;

    /** 統一的日期時間輸出格式，格式為 {@code yyyy/MM/dd HH:mm:ss} */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * 無參數建構子，供 Jackson 透過反射自動實例化使用。
     */
    public DateSerializer() {
        this((Class)null);
    }

    /**
     * 指定目標型別的建構子。
     *
     * @param t 要序列化的目標型別（通常為 {@code Date.class}）
     */
    public DateSerializer(Class<Date> t) {
        super(t);
    }

    /**
     * 將 {@link Date} 物件序列化為格式化的日期時間字串並寫入 JSON 輸出流。
     *
     * @param date                 要序列化的日期物件
     * @param jsonGenerator        Jackson JSON 輸出產生器
     * @param serializationContext Jackson 序列化上下文（本方法未使用，但為覆寫介面所必要）
     */
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        // 將 Date 格式化為字串後直接寫入 JSON，確保輸出格式一致
        jsonGenerator.writeString(dateFormat.format(date));
    }

}
