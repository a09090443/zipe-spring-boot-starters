package com.example.controller;

import com.zipe.base.controller.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Zipe
 */
@Controller
@RequestMapping("/")
public class WebController extends BaseController {

    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        return "html/hello";
    }

    @GetMapping({"/jsp"})
    public String jsp() {
        return "jsp/hello";
    }

    @GetMapping({"/demo"})
    public String demo() {
        return "html/demo";
    }

    @Override
    @GetMapping({"/"})
    public ModelAndView initPage() {
        ModelAndView view = new ModelAndView("index");
        view.addObject("name", "John");
        return view;
    }
}
