package com.example.service;

import com.example.base.TestBase;
import com.example.model.UserDetail;
import com.example.model.UserMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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

        UserMain main = new UserMain();
        main.setName("Gary");
        main = exampleServiceImpl.saveOrUpdateUser(main);

        UserDetail detail = new UserDetail();
        detail.setName(main.getName());
        detail.setGender("M");
        exampleServiceImpl.saveOrUpdateInfo(detail);
    }

    @AfterEach
    public void deleteRecord() {
        UserMain main = exampleServiceImpl.findUserByName("Gary");
        exampleServiceImpl.deleteUser(main);
        UserDetail detail = exampleServiceImpl.findByUserName(main.getName());
        exampleServiceImpl.deleteInfo(detail);
    }

    @Test
    public void findUserByNameTest() {
        UserMain main = exampleServiceImpl.findUserByName("Gary");
        Assertions.assertNotNull(main);
    }

    @Test
    public void updateUserAndInfoTest() {
        UserMain main = exampleServiceImpl.findUserByName("Gary");
        UserDetail detail = exampleServiceImpl.findByUserName(main.getName());
        String gender = "F";
        detail.setGender(gender);
        exampleServiceImpl.saveOrUpdateInfo(detail);
        UserDetail newInfo = exampleServiceImpl.findByUserName(main.getName());
        Assertions.assertEquals(newInfo.getGender(), gender);
    }

    @Test
    public void findUserByJdbcTest() {
        List<UserMain> users = exampleServiceImpl.findUserByJdbc("Jen");
        Assertions.assertNotNull(users);
    }

}
