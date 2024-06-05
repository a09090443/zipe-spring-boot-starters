package com.zipe.util;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

public class SoapUtil {

    // 取得 soap response 的 xml,並可指定 tagName
    public static String getResponseXml(String soapXml, String tagName){
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message;
            ByteArrayInputStream is = new ByteArrayInputStream(soapXml.getBytes());
            message = factory.createMessage(null, is);
            SOAPBody body = message.getSOAPBody();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            NodeList childNodes = body.getElementsByTagName(tagName);
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                transformer.transform(new DOMSource(childNode), new StreamResult(writer));
            }
            return writer.toString();
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
    public static String getFromSoapXml(String soapXml, String tagName) throws SOAPException, IOException, TransformerException {
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message;
            ByteArrayInputStream is = new ByteArrayInputStream(soapXml.getBytes());
            message = factory.createMessage(null, is);
            SOAPBody body = message.getSOAPBody();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            NodeList childNodes = body.getElementsByTagName(tagName);
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                transformer.transform(new DOMSource(childNode), new StreamResult(writer));
            }
            return writer.toString();
        } catch (Exception e) {
            throw e;
        }
    }
}
