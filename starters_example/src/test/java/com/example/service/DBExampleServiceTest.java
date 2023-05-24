package com.example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.base.TestBase;
import com.example.model.UserMain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DBExampleServiceTest extends TestBase {

  private final DBExampleService dbExampleService;

  @Autowired
  DBExampleServiceTest(DBExampleService dbExampleService){
    this.dbExampleService = dbExampleService;
  }

  @Test
  void findUserMainByNameTest(){
    UserMain userMain = dbExampleService.getUserMainByName("Gary");
    assertEquals("Gary", userMain.getName());
  }
}
