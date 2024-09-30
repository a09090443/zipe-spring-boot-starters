package com.example.webservice;

import com.zipe.model.User;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import java.util.Map;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/5/4 下午 02:05
 **/
//@WebService(targetNamespace="http://service.springboot.mracale.com")如果不添加的話,動態調用invoke的時候,會出現找不到接口內的方法,具體原因未知.
@WebService(name = "UserService", targetNamespace = "http://service.example.com")
public interface ExampleWebService {

    @WebMethod//標注該方法為webservice暴露的方法,用於向外公布，它修飾的方法是webservice方法，去掉也沒影響的，類似一個注釋信息。
    public User getUser(@WebParam(name = "userId", targetNamespace = "http://service.example.com") String userId);

    @WebMethod
    public String getUserName(@WebParam(name = "userId", targetNamespace = "http://service.example.com") String userId);

    @WebMethod
    @WebResult(name = "Map")
    public Map<String, User> getAllUserData();
}
