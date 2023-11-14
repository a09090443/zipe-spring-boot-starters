package com.example.webservice.impl;

import com.example.webservice.ExampleWebService;
import com.example.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.jws.WebService;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@WebService(serviceName = "ExampleWebService",//對外發布的服務名
        targetNamespace = "http://service.example.com/",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface = "com.example.webservice.ExampleWebService")
@Component
public class ExampleWebServiceImpl implements ExampleWebService {

    private final Map<String, User> userMap = new HashMap<>();

    @Override
    public String sayHello(String name) {
        log.info("Hello, {}!", name);
        return "Hello, " + name + "!";
    }
}
