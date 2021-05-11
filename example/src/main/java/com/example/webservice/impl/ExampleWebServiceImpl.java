package com.example.webservice.impl;

import com.example.webservice.ExampleWebService;
import org.springframework.stereotype.Component;

import javax.jws.WebService;

@WebService(serviceName = "ExampleWebService",//對外發布的服務名
        targetNamespace = "http://service.example.com",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface = "com.example.webservice.ExampleWebService")//服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
@Component
public class ExampleWebServiceImpl implements ExampleWebService {
    @Override
    public String helloWorld(String value) {
        String helloWorld = "Example say hello world to " + value;
        return helloWorld;
    }
}
