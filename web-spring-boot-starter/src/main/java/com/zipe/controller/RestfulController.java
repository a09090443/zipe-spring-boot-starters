package com.zipe.controller;

import com.zipe.dto.Result;
import com.zipe.dto.User;
import com.zipe.exception.ResultException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/rest", produces = "application/json")
public class RestfulController {

    private static final HashMap<String, Object> INFO;

    static {
        INFO = new HashMap<>();
        INFO.put("name", "galaxy");
        INFO.put("age", "70");
    }

    @GetMapping(value = "/hello")
    public HashMap<String, Object> hello() {
        return INFO;
    }

    @GetMapping("/result")
    public Result<Map<String, Object>> helloResult() {
        return Result.success(INFO);
    }

    @GetMapping("/helloError")
    public HashMap<String, Object> helloError() throws Exception {
        throw new Exception("helloError");
    }

    @GetMapping("/helloMyError")
    public HashMap<String, Object> helloMyError() throws Exception {
        throw new ResultException();
    }

    @GetMapping(value = "/testString")
    public String testString() {
        return "helloString";
    }

    @GetMapping(value = "/testInt")
    public Integer testInt() {
        return 123;
    }

    @GetMapping(value = "/testUser")
    public User testUser() {
        User user = new User();
        user.setUsername("Gary");
        user.setPassword("password");
        return user;
    }
}
