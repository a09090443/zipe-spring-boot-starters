package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.base.TestBase;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
class WebControllerTest extends TestBase {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void testThymeleaf() throws Exception {
    this.mockMvc.perform(get("/thymeleaf")).andExpect(status().isOk());
  }
  @Test
  void testJsp() throws Exception {
    this.mockMvc.perform(get("/jsp")).andExpect(status().isOk());
  }
}
