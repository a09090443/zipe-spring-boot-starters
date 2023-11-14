package com.example.controller;

import com.example.base.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ExampleControllerTest extends TestBase {
    private final MockMvc mockMvc;

    @Autowired
    ExampleControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void sayHelloTest() throws Exception {
        this.mockMvc.perform(get("/rest/sayHello?name=Gary")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello, Gary!")));
    }
}
