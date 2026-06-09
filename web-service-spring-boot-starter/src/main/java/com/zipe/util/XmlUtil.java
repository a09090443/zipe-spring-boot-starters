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

/**
 * XML 工具類別，提供 XML 與 Java 物件之間的序列化／反序列化功能。
 *
 * <p>底層使用 Jackson {@link XmlMapper}，並在建立時強制停用 DTD 與外部實體解析，
 * 以防範 XXE（XML External Entity）攻擊。全域共用單一 {@code xmlMapper} 實例，
 * 已預先設定日期格式、空 Bean 容錯及未知屬性忽略等常用選項。</p>
 */
public class XmlUtil {

    /** 全域共用的 XmlMapper 實例，底層 XmlFactory 已啟用 XXE 防護。 */
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
        // 序列化時始終輸出所有欄位，包含 null 值
        xmlMapper.setSerializationInclusion(Include.ALWAYS);
        // 設定日期欄位序列化與反序列化格式
        xmlMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        // 忽略空 Bean 序列化時的錯誤，避免無欄位的物件導致例外
        xmlMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 忽略未知屬性，防止 XML 字串存在而 Java 物件不存在的欄位時反序列化失敗
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 註冊 Java 8 時間模組，支援 LocalDate / LocalDateTime 等型別
        xmlMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 將 XML 字串反序列化為指定型別的 Java 物件。
     *
     * @param <T>   目標型別
     * @param xml   待反序列化的 XML 字串
     * @param clazz 目標類別的 {@link Class} 物件
     * @return 反序列化後的物件實例
     * @throws RuntimeException 若 XML 格式不合法或反序列化過程發生錯誤
     */
    public static <T> T xmlToBean(String xml, Class<T> clazz) {
        try {
            return xmlMapper.readValue(xml, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("XML 序列化失敗", e);
        }
    }

    /**
     * 將 Java 物件序列化為 XML 字串。
     *
     * @param obj 待序列化的物件
     * @return 序列化後的 XML 字串
     * @throws RuntimeException 若序列化過程發生錯誤
     */
    public static String beanToXml(Object obj) {
        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("XML 序列化失敗", e);
        }
    }
}
