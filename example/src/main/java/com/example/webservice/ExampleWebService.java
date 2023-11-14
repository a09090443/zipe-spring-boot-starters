package com.example.webservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(targetNamespace = "http://service.example.com/")
public interface ExampleWebService {

    @WebMethod//標注該方法為webservice暴露的方法,用於向外公布，它修飾的方法是webservice方法，去掉也沒影響的，類似一個注釋信息。
    public String sayHello(@WebParam(name = "name", targetNamespace = "http://service.example.com/") String name);
}
