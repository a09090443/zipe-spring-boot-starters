package com.zipe.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * SOAP 訊息工具類別。
 *
 * <p>提供以下功能：
 * <ul>
 *   <li>以 HTTP POST 傳送 XML/SOAP 請求並取得回應字串</li>
 *   <li>從 SOAP 回應 XML 中擷取指定 tagName 的節點內容</li>
 * </ul>
 *
 * <p>所有 XML 解析與轉換均採用安全設定（停用 DTD 與外部實體），
 * 以防範 XXE（XML External Entity）注入攻擊。
 */
public class SoapUtil {

    /**
     * 建立已停用 DTD 與外部實體的安全 TransformerFactory，避免 XXE 攻擊。
     *
     * <p>透過 FEATURE_SECURE_PROCESSING、ACCESS_EXTERNAL_DTD、ACCESS_EXTERNAL_STYLESHEET
     * 三項設定，禁止存取外部 DTD 與樣式表，防止外部實體注入與 SSRF。</p>
     */
    private static TransformerFactory newSecureTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (Exception e) {
            // 某些實作可能不支援部分屬性；仍以已套用之安全設定為主
            throw new RuntimeException("無法建立安全的 TransformerFactory", e);
        }
        return transformerFactory;
    }

    /**
     * 建立已停用 DTD 與外部實體的安全 DocumentBuilder，避免 XXE 攻擊。
     *
     * <p>解析不可信的 SOAP XML 時，停用 DOCTYPE 與外部實體，從源頭杜絕
     * 外部實體注入（讀取本機檔案 / SSRF / DoS）。</p>
     */
    private static DocumentBuilder newSecureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // 全面禁止 DOCTYPE，含 DTD 的不可信 XML 一律拒絕
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // 停用外部一般實體與參數實體
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // 不載入外部 DTD
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }

    /**
     * 以安全方式解析 SOAP XML 為 DOM 文件（停用 DTD/外部實體）。
     */
    private static Document parseSecureDocument(String soapXml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = newSecureDocumentBuilder();
        try (ByteArrayInputStream is =
                     new ByteArrayInputStream(soapXml.getBytes(StandardCharsets.UTF_8))) {
            return builder.parse(is);
        }
    }

    /**
     * 將指定 tagName 的節點轉為 XML 字串（共用實作，使用安全的 Transformer/DocumentBuilder）。
     */
    private static String extractTagAsXml(String soapXml, String tagName)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Document document = parseSecureDocument(soapXml);

        TransformerFactory transformerFactory = newSecureTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        NodeList childNodes = document.getElementsByTagName(tagName);
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            transformer.transform(new DOMSource(childNode), new StreamResult(writer));
        }
        return writer.toString();
    }

    /**
     * 從 SOAP 回應 XML 中擷取指定 tagName 的節點，並以 XML 字串回傳。
     *
     * <p>內部使用安全的 DocumentBuilder 與 TransformerFactory，避免 XXE 攻擊。
     * 若解析過程發生例外，將包裝為 {@link RuntimeException} 拋出。
     *
     * @param soapXml  完整的 SOAP XML 回應字串
     * @param tagName  欲擷取的 XML 標籤名稱
     * @return 符合 tagName 的所有節點序列化後的 XML 字串
     * @throws RuntimeException 當 XML 解析或轉換失敗時
     */
    // 取得 soap response 的 xml,並可指定 tagName
    public static String getResponseXml(String soapXml, String tagName) {
        try {
            return extractTagAsXml(soapXml, tagName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 以 HTTP POST 方式傳送 XML 內容至指定 URL，並回傳回應字串。
     *
     * <p>使用 Apache HttpClient 5 傳送請求，Content-Type 設為 {@code application/xml}，
     * 回應內容以 UTF-8 編碼解析。HttpClient 與回應資源透過 try-with-resources 與
     * response handler 自動關閉。
     *
     * @param url 目標服務的 URL 字串
     * @param xml 欲傳送的 XML 請求主體（通常為 SOAP Envelope）
     * @return 伺服器回應的字串內容
     * @throws IOException 當 HTTP 連線、傳送或關閉資源失敗時
     */
    public static String doPostWithXml(String url, String xml) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 建立 HTTP POST 請求，主體為 application/xml 的 SOAP Envelope
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(xml, ContentType.APPLICATION_XML));
            // 以 response handler 取出回應字串，HttpClient 5 會自動關閉回應資源
            return httpClient.execute(httpPost, response ->
                    EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        }
    }

    /**
     * 取得 soap response 的 xml, 並可指定 tagName
     */
    public static String getFromSoapXml(String soapXml, String tagName)
            throws IOException, TransformerException, ParserConfigurationException, SAXException {
        return extractTagAsXml(soapXml, tagName);
    }
}
