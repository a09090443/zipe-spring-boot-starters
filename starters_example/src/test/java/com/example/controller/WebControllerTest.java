package com.example.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.base.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
class WebControllerTest extends TestBase {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testThymeleaf() throws Exception {
    this.mockMvc.perform(get("/thymeleaf").with(httpBasic("admin", "admin"))).andExpect(status().isOk());
  }
  @Test
  void testJsp() throws Exception {
    this.mockMvc.perform(get("/jsp").with(httpBasic("admin", "admin"))).andExpect(status().isOk());
  }
}
