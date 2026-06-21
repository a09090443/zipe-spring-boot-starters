package com.zipe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 驗證 SoapUtil 與 XmlUtil 對 XXE（XML 外部實體）攻擊具備防護能力的測試。
 *
 * <p>防護策略為「停用 DTD 與外部實體」：SoapUtil 以 DOM 解析時直接拒絕 DOCTYPE，
 * XmlUtil 則關閉底層 XMLInputFactory 的 DTD 與外部實體處理。兩者皆不會解析外部實體，
 * 確保機敏檔案內容不會經由實體注入外洩，正常 XML 仍可正確解析。</p>
 *
 * @author : Gary Tsai
 **/
class XxeSecurityTest {

    /** 內含機敏內容的本機檔案，用以驗證外部實體是否被解析讀取 */
    private File secretFile;

    /** 機敏檔案內的標記字串；若外部實體被解析，此字串會出現在輸出中 */
    private static final String SECRET_MARKER = "TOP_SECRET_PASSWORD_1234";

    @BeforeEach
    void setUp() throws Exception {
        // 建立一個暫存檔，內含可被偵測的機敏字串
        secretFile = File.createTempFile("xxe-secret", ".txt");
        Files.write(secretFile.toPath(), SECRET_MARKER.getBytes(StandardCharsets.UTF_8));
        secretFile.deleteOnExit();
    }

    /**
     * 建立含有外部實體（讀取本機檔案）的惡意 SOAP 訊息。
     */
    private String maliciousSoapXml() {
        String fileUri = secretFile.toURI().toString();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE soap:Envelope [ <!ENTITY xxe SYSTEM \"" + fileUri + "\"> ]>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + "<result>&xxe;</result>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /**
     * 一份正常、不含外部實體的 SOAP 訊息，用以驗證正常流程仍可運作。
     */
    private String normalSoapXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + "<result>hello</result>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /** 取得例外鏈中所有訊息串接後的字串，用以檢查機敏內容是否外洩。 */
    private static String allMessages(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getClass().getName()).append(':').append(c.getMessage()).append('\n');
        }
        return sb.toString();
    }

    /** 取得例外鏈的根因。 */
    private static Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        return c;
    }

    // ===== SoapUtil.getResponseXml =====

    @Test
    void getResponseXml_shouldRejectDoctypeAndNotLeak() {
        // 惡意 XML 不得讓機敏檔案內容外洩；停用 DTD 後應因 DOCTYPE 被拒而拋出受控例外。
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> SoapUtil.getResponseXml(maliciousSoapXml(), "result"));
        assertFalse(allMessages(ex).contains(SECRET_MARKER),
                "例外訊息不得包含機敏檔案內容");
        // 確認是因為 DOCTYPE 被禁止而拒絕，證明 DTD 防護確實生效
        assertTrue(rootCause(ex).getMessage().contains("DOCTYPE"),
                "應因 DOCTYPE 被停用而拒絕解析");
    }

    @Test
    void getResponseXml_shouldParseNormalXml() {
        String result = SoapUtil.getResponseXml(normalSoapXml(), "result");
        assertNotNull(result, "正常 XML 應可被解析");
        assertTrue(result.contains("hello"), "正常 XML 的內容應被保留");
    }

    // ===== SoapUtil.getFromSoapXml =====

    @Test
    void getFromSoapXml_shouldRejectDoctypeAndNotLeak() {
        // 含 DOCTYPE 的惡意 SOAP 應因 DTD 被停用而安全拒絕，且不得外洩機敏內容。
        Exception ex = assertThrows(Exception.class,
                () -> SoapUtil.getFromSoapXml(maliciousSoapXml(), "result"));
        assertFalse(allMessages(ex).contains(SECRET_MARKER),
                "例外訊息不得包含機敏檔案內容");
        assertTrue(rootCause(ex).getMessage().contains("DOCTYPE"),
                "應因 DOCTYPE 被停用而拒絕解析");
    }

    @Test
    void getFromSoapXml_shouldParseNormalXml() throws Exception {
        String result = SoapUtil.getFromSoapXml(normalSoapXml(), "result");
        assertTrue(result.contains("hello"), "正常 XML 的內容應被保留");
    }

    // ===== XmlUtil.xmlToBean =====

    public static class XxeBean {
        public String value;
    }

    /** 含外部實體（讀取本機檔案）的惡意 Bean XML */
    private String maliciousBeanXml() {
        String fileUri = secretFile.toURI().toString();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE XxeBean [ <!ENTITY xxe SYSTEM \"" + fileUri + "\"> ]>"
                + "<XxeBean><value>&xxe;</value></XxeBean>";
    }

    @Test
    void xmlToBean_shouldNotLeakExternalEntity() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> XmlUtil.xmlToBean(maliciousBeanXml(), XxeBean.class),
                "含外部實體的 XML 應被拒絕，避免機敏內容外洩");
        assertFalse(String.valueOf(ex.getMessage()).contains(SECRET_MARKER),
                "例外訊息不得包含機敏檔案內容");
    }

    @Test
    void xmlToBean_shouldParseNormalXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<XxeBean><value>hello</value></XxeBean>";
        XxeBean bean = XmlUtil.xmlToBean(xml, XxeBean.class);
        assertEquals("hello", bean.value, "正常 XML 應可正確反序列化");
    }
}
