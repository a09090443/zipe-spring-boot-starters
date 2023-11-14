package com.example.webservice;

import com.example.webservice.dto.UserRequest;
import com.example.webservice.dto.UserResponse;
import com.zipe.model.User;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import java.util.Map;

@WebService(targetNamespace = "http://service.user.com/")
public interface ExampleUserService {

    @WebMethod//標注該方法為webservice暴露的方法,用於向外公布，它修飾的方法是webservice方法，去掉也沒影響的，類似一個注釋信息。
    @WebResult(name = "userResult")
    public UserResponse getUser(@WebParam(name = "User", targetNamespace = "http://service.user.com/") UserRequest userRequest);

    @WebMethod
    @WebResult(name = "String", targetNamespace = "")
    public String getUserId(@WebParam(name = "name", targetNamespace = "http://service.user.com/") String name);

    @WebMethod
    @WebResult(name = "Map")
    public Map<String, User> getAllUserData();
}
