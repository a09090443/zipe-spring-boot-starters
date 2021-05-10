package com.example.controller;

import com.zipe.base.controller.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Gary.Tsai
 */
@Controller
@RequestMapping({"/"})
public class ExampleController extends BaseController {

    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        return "html/hello";
    }

    @GetMapping({"/jsp"})
    public String jsp() {
        return "jsp/hello";
    }

    @Override
    public org.springframework.web.servlet.ModelAndView initPage() {
        return null;
    }
}
