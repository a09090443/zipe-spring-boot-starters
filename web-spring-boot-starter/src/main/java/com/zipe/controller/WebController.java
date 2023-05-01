package com.zipe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class WebController {

    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        return "html/hello";
    }

    @GetMapping({"/jsp"})
    public String jsp() {
        return "jsp/hello";
    }
}
