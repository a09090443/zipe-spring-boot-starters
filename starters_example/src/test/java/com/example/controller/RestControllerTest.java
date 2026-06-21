package com.example.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.base.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
class RestControllerTest extends TestBase {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void sayHelloTest() throws Exception {
    // BASIC 模式以 user01/1234 認證後存取受保護端點（帳密來自 security.basic.users）
    this.mockMvc.perform(get("/rest/sayHello?name=John").with(httpBasic("user01", "1234")))
        .andDo(print()).andExpect(status().isOk())
        .andExpect(content().string(containsString("Hello,  John!")));
  }
}
