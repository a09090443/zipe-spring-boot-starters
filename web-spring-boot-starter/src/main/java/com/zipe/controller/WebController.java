package com.zipe.controller;

import com.zipe.base.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author : Gary Tsai
 */
@Controller
@RequestMapping("/")
public class WebController extends BaseController {

    @Autowired
    WebController(Environment env, MessageSource messageSource, HttpServletRequest request,
        HttpServletResponse response) {
        super(env, messageSource, request, response);
    }

    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        return "html/hello";
    }

    @GetMapping({"/jsp"})
    public String jsp() {
        return "jsp/hello";
    }

    @Override
    @RequestMapping("/")
    public ModelAndView initPage() {
        ModelAndView modelAndView = new ModelAndView("message");
        modelAndView.addObject("message", "I'm a joke!!");
        return modelAndView;
    }
}
