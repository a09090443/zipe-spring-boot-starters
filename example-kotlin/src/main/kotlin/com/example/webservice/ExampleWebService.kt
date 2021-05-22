package com.example.webservice

import javax.jws.WebMethod
import javax.jws.WebService

@WebService(targetNamespace = "http://service.example.com")
interface ExampleWebService {
    @WebMethod(operationName = "helloWorld")
    fun helloWorld(value: String): String
}
