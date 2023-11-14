package com.example.service;

import com.example.base.TestBase;
import com.example.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.List;

public class ExampleServiceTest extends TestBase {
    private final ExampleService exampleService;

    @Autowired
    ExampleServiceTest(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    @Test
    public void findUsersTest() {
        List<User> userList = exampleService.findUsers();
        Assert.notEmpty(userList);
    }
}
