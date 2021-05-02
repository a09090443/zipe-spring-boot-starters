package com.zipe.service;


import com.zipe.model.User;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface UserService {

    @WebMethod
    String getName(@WebParam(name = "userId") Long userId);

    @WebMethod
    User getUser(Long userId);

}
