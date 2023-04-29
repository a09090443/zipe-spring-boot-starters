package com.example.controller;

import com.example.base.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestSecurityTest extends TestBase {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final String USERNAME = "admin";
    private final String PASSWORD = "admin";

    @BeforeEach
    public void getAccess() {
        log.debug("測試使用 port:{}", port);
        // 如有登入需求，需輸入帳號密碼
        testRestTemplate = testRestTemplate.withBasicAuth(USERNAME, PASSWORD);
    }

    @Test
    void sayHelloTest() {
        String feedback = testRestTemplate.getForObject("/rest/sayHello", String.class);
        Assertions.assertEquals(feedback, "Hello");
    }
}
