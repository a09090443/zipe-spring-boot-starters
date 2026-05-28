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
 * @author zipe1
 * @created 2024/10/29
 */
public class ResponseCdataInterceptor extends AbstractSoapInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ResponseCdataInterceptor.class);

    public ResponseCdataInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(SoapMessage message) {
        try {
            // 獲取輸出流
            OutputStream os = message.getContent(OutputStream.class);
            if (os == null) {
                logger.debug("OutputStream is null, trying other content types");
                return;
            }

            // 創建緩存輸出流
            CachedOutputStream cos = new CachedOutputStream();
            message.setContent(OutputStream.class, cos);

            // 等待原始消息寫入完成
            message.getInterceptorChain().doIntercept(message);

            // 獲取內容
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
        // 處理 CDATA 內容
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
