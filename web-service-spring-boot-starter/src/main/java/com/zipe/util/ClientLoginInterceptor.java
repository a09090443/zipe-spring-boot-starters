package com.zipe.util;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * 用於調用webservices接口的安全驗證攔截器
 *
 * @author Gary Tsai
 */
public class ClientLoginInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public ClientLoginInterceptor(String userName, String userPassword) {
        super(Phase.PREPARE_SEND);
        this.userName = userName;
        this.userPassword = userPassword;
    }

    private String userName;
    private String userPassword;

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

        headers.add(0, new Header(new QName("SecurityHeader"), auth));
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

}
