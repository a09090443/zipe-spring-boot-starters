package com.example.webservice.impl;

import com.example.webservice.ExampleWebService;
import com.zipe.util.time.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.jws.WebService;

/**
 * @author Gary.Tsai
 */
@WebService(serviceName="ExampleService",//對外發布的服務名
        targetNamespace="http://service.example.com",//指定你想要的名稱空間，通常使用使用包名反轉
        endpointInterface="com.example.webservice.ExampleWebService")//服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
@Component
@Slf4j
public class ExampleWebServiceImpl implements ExampleWebService {
    @Override
    public void reportDateTime() {
        log.info("現在時間:{}", DateTimeUtils.getDateNow());
    }
}
