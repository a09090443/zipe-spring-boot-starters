package com.zipe.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

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

    // 取得 soap response 的 xml,並可指定 tagName
    public static String getResponseXml(String soapXml, String tagName) {
        try {
            return extractTagAsXml(soapXml, tagName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String doPostWithXml(String url, String xml) throws IOException {
        // Create Httpclient object
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            // Create Http Post request
            HttpPost httpPost = new HttpPost(url);
            // Create StringEntity with the XML content
            StringEntity entity = new StringEntity(xml, ContentType.APPLICATION_XML);
            httpPost.setEntity(entity);
            // Execute http request
            response = httpClient.execute(httpPost);
            resultString = EntityUtils.toString(response.getEntity(), "utf-8");
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            } catch (IOException e) {
                throw e;
            }
        }

        return resultString;
    }

    /**
     * 取得 soap response 的 xml, 並可指定 tagName
     */
    public static String getFromSoapXml(String soapXml, String tagName)
            throws IOException, TransformerException, ParserConfigurationException, SAXException {
        return extractTagAsXml(soapXml, tagName);
    }
}
