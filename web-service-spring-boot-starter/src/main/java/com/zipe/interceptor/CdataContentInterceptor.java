package com.zipe.interceptor;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author zipe1
 * @created 2024/10/29
 */
public class CdataContentInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger logger = LoggerFactory.getLogger(CdataContentInterceptor.class);

    public CdataContentInterceptor() {
        // 使用 RECEIVE 階段
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(Message message) {
        try {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                logger.debug("開始處理消息內容");

                // 讀取並緩存內容
                CachedOutputStream cos = new CachedOutputStream();
                IOUtils.copy(is, cos);
                is.close();

                // 獲取內容字符串
                String content = new String(cos.getBytes(), "UTF-8");
                logger.debug("原始內容: {}", content);

                // 處理轉義的 CDATA 內容
                String processedContent = processCdataContent(content);
                logger.debug("處理後內容: {}", processedContent);

                // 將處理後的內容放回消息
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        processedContent.getBytes("UTF-8"));
                message.setContent(InputStream.class, bais);

                cos.close();
            } else {
                logger.warn("消息內容為空");
            }
        } catch (Exception e) {
            logger.error("處理 CDATA 內容時發生錯誤", e);
            throw new RuntimeException("處理 CDATA 內容時發生錯誤", e);
        }
    }

    private String processCdataContent(String content) {
        // 處理轉義的 CDATA 和 XML 標籤
        return content
                .replaceAll("&lt;!\\[CDATA\\[", "<![CDATA[")
                .replaceAll("\\]\\]&gt;", "]]>")
                .replaceAll("&lt;email&gt;", "<email>")
                .replaceAll("&lt;/email&gt;", "</email>")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">");
    }
}
