package com.example.service;

import com.example.model.Info;
import com.example.model.User;

import java.util.List;

/**
 * @author Gary.Tsai
 */
public interface ExampleService {

    User findUserById(Integer id);

    User findUserByName(String name);

    List<User> findAll();

    User saveOrUpdateUser(User user);

    void deleteUser(User user);

    Info findByUserId(Integer userId);

    Info saveOrUpdateInfo(Info info);

    void deleteInfo(Info info);
}
