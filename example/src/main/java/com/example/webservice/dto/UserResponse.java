package com.example.webservice.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import javax.xml.bind.annotation.XmlElement;

@Data
@JacksonXmlRootElement(localName = "userResult")
public class UserResponse {
    /**
     * user id
     */
    @JacksonXmlProperty(localName = "UserId")
    @JacksonXmlCData
    private String userId;
    /**
     * username
     */
    @JacksonXmlProperty(localName = "UserName")
    @JacksonXmlCData
    private String name;
    /**
     * email
     */
    @JacksonXmlProperty(localName = "Email")
    @JacksonXmlCData
    private String email;

    @XmlElement(namespace = "http://service.user.com/", name = "id")
    public void setUserId(String userId) {
        this.userId = userId;
    }
    @XmlElement(namespace = "http://service.user.com/", name = "name")
    public void setName(String name) {
        this.name = name;
    }
    @XmlElement(namespace = "http://service.user.com/", name = "email")
    public void setEmail(String email) {
        this.email = email;
    }
}
