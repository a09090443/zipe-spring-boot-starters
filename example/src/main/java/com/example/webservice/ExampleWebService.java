package com.example.webservice;

import javax.jws.WebService;

@WebService(targetNamespace = "http://service.example.com")
public interface ExampleWebService {
    String helloWorld(String value);
}
