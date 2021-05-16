package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Gary Tsai
 */
@RestController
@RequestMapping("/rest")
public class RestExampleController {

    @GetMapping("/sayHello")
    public String sayHello(){
        return "Hello";
    }
}
