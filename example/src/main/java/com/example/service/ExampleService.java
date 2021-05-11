package com.example.service;

import com.example.model.UserDetail;
import com.example.model.UserMain;

import java.util.List;

/**
 * @author Gary.Tsai
 */
public interface ExampleService {

    UserMain findUserById(Integer id);

    UserMain findUserByName(String name);

    List<UserMain> findAll();

    UserMain saveOrUpdateUser(UserMain userMain);

    void deleteUser(UserMain user);

    UserDetail findByUserName(String name);

    UserDetail saveOrUpdateInfo(UserDetail userDetail);

    void deleteInfo(UserDetail userDetail);

    List<UserMain> findUserByJdbc(String name);
}
