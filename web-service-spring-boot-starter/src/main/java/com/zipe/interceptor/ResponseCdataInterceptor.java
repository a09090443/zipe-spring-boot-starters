package com.zipe.interceptor;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * CXF SOAP 回應攔截器，負責將回應 XML 中被 HTML 實體編碼的 CDATA 標記與特殊字元
 * 還原為原始符號，確保下游用戶端能正確解析含 CDATA 區段的 SOAP 回應訊息。
 * <p>
 * 攔截時機為 {@link Phase#PRE_STREAM}，在回應寫出至網路前執行轉換。
 * </p>
 *
 * @author zipe1
 * @created 2024/10/29
 */
public class ResponseCdataInterceptor extends AbstractSoapInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ResponseCdataInterceptor.class);

    /**
     * 建立攔截器，並將執行階段設定為 {@link Phase#PRE_STREAM}，
     * 確保在 SOAP 訊息序列化寫出前進行 CDATA 還原處理。
     */
    public ResponseCdataInterceptor() {
        super(Phase.PRE_STREAM);
    }

    /**
     * 攔截 SOAP 回應訊息，將回應內容中被實體編碼的 CDATA 標記與 XML 特殊字元
     * 還原為原始符號後，再寫回輸出流。
     *
     * @param message 目前正在處理的 {@link SoapMessage}，包含回應內容與輸出流
     * @throws RuntimeException 當讀取或寫入輸出流發生 I/O 例外時拋出
     */
    @Override
    public void handleMessage(SoapMessage message) {
        try {
            // 取得輸出流
            OutputStream os = message.getContent(OutputStream.class);
            if (os == null) {
                logger.debug("OutputStream is null, trying other content types");
                return;
            }

            // 建立快取輸出流，暫存原始 SOAP 回應內容以便後續讀取與改寫
            CachedOutputStream cos = new CachedOutputStream();
            message.setContent(OutputStream.class, cos);

            // 等待原始訊息寫入完成（繼續執行後續攔截器鏈，直到 SOAP 訊息完整寫入 cos）
            message.getInterceptorChain().doIntercept(message);

            // 取得內容
            String content = IOUtils.toString(cos.getInputStream(), StandardCharsets.UTF_8);
            logger.debug("Original content: {}", content);

            // 處理內容
            String processedContent = processCdataContent(content);
            logger.debug("Processed content: {}", processedContent);

            // 寫回輸出流
            IOUtils.copy(new ByteArrayInputStream(processedContent.getBytes(StandardCharsets.UTF_8)), os);
            cos.close();

        } catch (Exception e) {
            logger.error("Error processing CDATA content", e);
            throw new RuntimeException("Error processing CDATA content", e);
        }
    }

    private String processCdataContent(String content) {
        // 將 XML 實體編碼的 CDATA 標記、email 標籤及常見特殊字元逐一還原為原始符號，
        // 順序由特定標記（CDATA、email）優先，再處理通用的 &lt; / &gt; / &amp;，
        // 避免替換順序錯誤導致字串二次轉換
        content = content.replaceAll("&lt;!\\[CDATA\\[", "<![CDATA[")
                .replaceAll("\\]\\]&gt;", "]]>")
                .replaceAll("&lt;email&gt;", "<email>")
                .replaceAll("&lt;/email&gt;", "</email>")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");

        logger.debug("After processing: {}", content);
        return content;
    }
}
