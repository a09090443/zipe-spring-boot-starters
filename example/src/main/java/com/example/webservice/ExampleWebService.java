package com.example.webservice;

import jakarta.jws.WebService;

@WebService(targetNamespace = "http://service.example.com")
public interface ExampleWebService {
    String helloWorld(String value);
}
