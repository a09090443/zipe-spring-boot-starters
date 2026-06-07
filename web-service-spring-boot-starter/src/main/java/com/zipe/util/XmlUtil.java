package com.zipe.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.xml.stream.XMLInputFactory;
import java.text.SimpleDateFormat;

public class XmlUtil {

    private static final XmlMapper xmlMapper = new XmlMapper(buildSecureXmlFactory());

    /**
     * 建立已停用 DTD 與外部實體的安全 XmlFactory，避免 XXE（XML 外部實體）攻擊。
     *
     * <p>反序列化不可信 XML 時，若允許 DTD 或外部實體，攻擊者可藉由 DOCTYPE/ENTITY
     * 讀取本機檔案或發動 SSRF/DoS。此處對底層 XMLInputFactory 關閉
     * SUPPORT_DTD 與 IS_SUPPORTING_EXTERNAL_ENTITIES，從源頭杜絕風險。</p>
     */
    private static XmlFactory buildSecureXmlFactory() {
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        // 停用 DTD 處理：DOCTYPE 內宣告的實體不會被處理，杜絕內部子集的實體展開攻擊
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // 停用外部實體解析，避免讀取本機檔案或外部資源（SSRF）
        inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return XmlFactory.builder()
                .inputFactory(inputFactory)
                .build();
    }

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
