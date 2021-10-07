package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Gary.Tsai
 */
@Slf4j
@Controller
@RequestMapping({"/"})
public class ExampleController {

    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        log.info("thymeleaf");
        return "html/hello";
    }

    @GetMapping({"/jsp"})
    public String jsp() {
        log.info("jsp");
        return "jsp/hello";
    }

    @GetMapping({"/demo"})
    public String demo() {
        log.info("webjars");
        return "html/demo";
    }

}
