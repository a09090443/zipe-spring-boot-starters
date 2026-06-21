package com.zipe.util;

import java.util.List;
import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 用於呼叫 WebService 介面時注入帳號密碼的安全驗證攔截器。
 *
 * <p>此攔截器在 {@link Phase#PREPARE_SEND} 階段將帳號與密碼封裝成
 * SOAP Header（SecurityHeader），插入至訊息標頭最前端，供服務端
 * 進行身分驗證。
 *
 * @author Gary Tsai
 */
public class ClientLoginInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    /** 呼叫 WebService 所使用的帳號 */
    private String userName;

    /** 呼叫 WebService 所使用的密碼 */
    private String userPassword;

    /**
     * 建構攔截器並設定帳號與密碼。
     *
     * <p>攔截器會在 {@link Phase#PREPARE_SEND} 階段執行，
     * 確保安全標頭在訊息送出前已附加完成。
     *
     * @param userName     呼叫端帳號
     * @param userPassword 呼叫端密碼
     */
    public ClientLoginInterceptor(String userName, String userPassword) {
        super(Phase.PREPARE_SEND);
        this.userName = userName;
        this.userPassword = userPassword;
    }

    /**
     * 處理 SOAP 訊息，將帳號密碼以 XML 元素方式嵌入 SOAP Header。
     *
     * <p>建立 {@code <authrity>} 節點，包含 {@code <userName>} 與
     * {@code <userPassword>} 子元素，並以 {@code SecurityHeader} 為
     * QName 插入至標頭列表最前端（index 0），使服務端能優先解析驗證資訊。
     *
     * @param soap 當前的 SOAP 訊息物件
     * @throws Fault 若訊息處理過程中發生錯誤
     */
    @Override
    public void handleMessage(SoapMessage soap) throws Fault {
        List<Header> headers = soap.getHeaders();
        Document doc = DOMUtils.createDocument();
        Element auth = doc.createElement("authrity");
        Element userName = doc.createElement("userName");
        Element userPassword = doc.createElement("userPassword");

        userName.setTextContent(this.userName);
        userPassword.setTextContent(this.userPassword);

        auth.appendChild(userName);
        auth.appendChild(userPassword);

        // 插入至標頭列表最前端，確保服務端能第一時間讀取驗證資訊
        headers.add(0, new Header(new QName("SecurityHeader"), auth));
    }

    /**
     * 取得呼叫端帳號。
     *
     * @return 帳號字串
     */
    public String getUserName() {
        return userName;
    }

    /**
     * 設定呼叫端帳號。
     *
     * @param userName 帳號字串
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 取得呼叫端密碼。
     *
     * @return 密碼字串
     */
    public String getUserPassword() {
        return userPassword;
    }

    /**
     * 設定呼叫端密碼。
     *
     * @param userPassword 密碼字串
     */
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

}
