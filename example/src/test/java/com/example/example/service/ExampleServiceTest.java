package com.example.example.service;

import com.example.example.base.TestBase;
import com.example.model.Info;
import com.example.model.User;
import com.example.service.ExampleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

/**
 * @author : Gary Tsai
 **/
public class ExampleServiceTest extends TestBase {

    public final ExampleService exampleServiceImpl;

    @Autowired
    public ExampleServiceTest(ExampleService exampleServiceImpl) {
        this.exampleServiceImpl = exampleServiceImpl;
    }

    @BeforeEach
    public void insertNewRecord() {

        User user = new User();
        user.setName("Gary");
        user = exampleServiceImpl.saveOrUpdateUser(user);

        Info info = new Info();
        info.setUserId(user.getId());
        info.setGender("M");
        exampleServiceImpl.saveOrUpdateInfo(info);
    }

    @AfterEach
    public void deleteRecord() {
        User user = exampleServiceImpl.findUserByName("Gary");
        exampleServiceImpl.deleteUser(user);
        Info info = exampleServiceImpl.findByUserId(user.getId());
        exampleServiceImpl.deleteInfo(info);
    }

    @Test
    public void findGleepfTest() {
        User user = exampleServiceImpl.findUserByName("Gary");
        Assertions.assertNotNull(user);
    }

    @Test
    public void updateGleepfTest() {
        User user = exampleServiceImpl.findUserByName("Gary");
        Info info = exampleServiceImpl.findByUserId(user.getId());
        String gender = "F";
        info.setGender(gender);
        exampleServiceImpl.saveOrUpdateInfo(info);
        Info newInfo = exampleServiceImpl.findByUserId(user.getId());
        Assertions.assertEquals(newInfo.getGender(), gender);
    }

}
