package com.example.service;

import com.example.model.User;

import java.util.List;

public interface ExampleService {

    List<User> findUsers();

    void findExample1Data();

    void findExample2Data();

    void findDb2Data();

    void findByNativeSQL(String name);

    String testException();
}
