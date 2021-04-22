package com.zipe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/21 上午 09:54
 **/
@Controller
@RequestMapping("/")
public class TestController {
    @GetMapping("/thhello")
    public String thHello() {
        return "th/hello";
    }

    @GetMapping("/jsphello")
    public String jspHello() {
        return "jsp/hello";
    }

    @GetMapping("/message")
    public String message(Model model) {
        model.addAttribute("message", "This is a custom message");
        return "th/message";
    }
}
