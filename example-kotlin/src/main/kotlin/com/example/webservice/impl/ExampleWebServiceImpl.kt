package com.example.webservice.impl

import com.example.webservice.ExampleWebService
import org.springframework.stereotype.Component
import javax.jws.WebService

@WebService(
    serviceName = "ExampleWebService", //服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
    targetNamespace = "http://service.example.com", //服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
    endpointInterface = "com.example.webservice.ExampleWebService" //服務接口全路徑, 指定做SEI（Service EndPoint Interface）服務端點接口
)
@Component
class ExampleWebServiceImpl : ExampleWebService {
    override fun helloWorld(value: String) = "Example say hello world to $value"
}
