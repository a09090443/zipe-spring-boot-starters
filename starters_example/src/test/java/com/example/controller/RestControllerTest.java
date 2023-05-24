package com.example.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.base.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
class RestControllerTest extends TestBase {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void sayHelloTest() throws Exception {
    this.mockMvc.perform(get("/rest/sayHello?name=John")).andDo(print()).andExpect(status().isOk())
        .andExpect(content().string(containsString("Hello,  John!")));
  }
}
