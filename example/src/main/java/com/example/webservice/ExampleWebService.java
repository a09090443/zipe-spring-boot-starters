package com.example.webservice;

import javax.jws.WebService;

/**
 * @author Gary.Tsai
 */
@WebService(targetNamespace="http://service.example.com")
public interface ExampleWebService {

    public void reportDateTime();
}
