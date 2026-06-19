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
 * CXF 入站攔截器，用於還原 SOAP 訊息中被 HTML 實體編碼轉義的 CDATA 區段與 XML 標籤。
 * <p>
 * 當客戶端或代理層將 {@code <![CDATA[...]]>} 中的 {@code &lt;} / {@code &gt;}
 * 轉義後，CXF 無法正確解析 XML 內容。本攔截器在 {@link Phase#RECEIVE} 階段介入，
 * 讀取原始訊息流並以正規表達式批量還原，再將修正後的串流重新注入訊息，讓後續
 * 解析鏈得以正常運作。
 * </p>
 *
 * @author zipe1
 * @created 2024/10/29
 */
public class CdataContentInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger logger = LoggerFactory.getLogger(CdataContentInterceptor.class);

    /**
     * 建立攔截器並指定在 {@link Phase#RECEIVE} 階段執行，
     * 確保在訊息解碼之前優先處理 CDATA 還原。
     */
    public CdataContentInterceptor() {
        // 使用 RECEIVE 階段
        super(Phase.RECEIVE);
    }

    /**
     * 攔截入站訊息，將訊息串流中被 HTML 實體編碼的 CDATA 區段與 XML 標籤還原為原始形式。
     *
     * @param message 當前 CXF 訊息物件，包含原始輸入串流
     * @throws RuntimeException 若讀取或寫入串流時發生 I/O 錯誤
     */
    @Override
    public void handleMessage(Message message) {
        try {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                logger.debug("開始處理訊息內容");

                // 先將輸入串流複製至 CachedOutputStream，避免串流只能讀取一次的限制
                CachedOutputStream cos = new CachedOutputStream();
                IOUtils.copy(is, cos);
                is.close();

                // 取得訊息內容字串，以便進行正規表達式替換
                String content = new String(cos.getBytes(), "UTF-8");
                logger.debug("原始內容: {}", content);

                // 處理轉義的 CDATA 內容
                String processedContent = processCdataContent(content);
                logger.debug("處理後內容: {}", processedContent);

                // 將處理後的內容重新包裝為 InputStream 並注回訊息，供後續攔截器使用
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        processedContent.getBytes("UTF-8"));
                message.setContent(InputStream.class, bais);

                cos.close();
            } else {
                logger.warn("訊息內容為空");
            }
        } catch (Exception e) {
            logger.error("處理 CDATA 內容時發生錯誤", e);
            throw new RuntimeException("處理 CDATA 內容時發生錯誤", e);
        }
    }

    private String processCdataContent(String content) {
        // 依序還原 CDATA 標記、email 標籤，最後處理其餘殘留的 &lt;/&gt; 實體，
        // 順序不可顛倒，以免已還原的 <![CDATA[...]]> 被後續規則再次誤判
        return content
                .replaceAll("&lt;!\\[CDATA\\[", "<![CDATA[")
                .replaceAll("\\]\\]&gt;", "]]>")
                .replaceAll("&lt;email&gt;", "<email>")
                .replaceAll("&lt;/email&gt;", "</email>")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">");
    }
}
