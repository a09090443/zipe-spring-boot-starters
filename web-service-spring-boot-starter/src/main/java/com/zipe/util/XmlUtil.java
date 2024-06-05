package com.zipe.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;

public class XmlUtil {

    private static final XmlMapper xmlMapper = new XmlMapper();

    static {
        // 對象為空時不列入
        xmlMapper.setSerializationInclusion(Include.ALWAYS);
        // 設置日期屬性序列化及反序列化格式
        xmlMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        // 忽略空Bean轉json的錯誤
        xmlMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 忽略未知屬性, 防止Json 字符串存在, Java 對象不存在的屬性時,反序列化失敗
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 註冊時間模組
        xmlMapper.registerModule(new JavaTimeModule());
    }

    public static <T> T xmlToBean(String xml, Class<T> clazz) {
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("XML 序列化失敗", e);
        }
    }

    public static String beanToXml(Object obj) {
        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("XML 序列化失敗", e);
        }
    }
}
