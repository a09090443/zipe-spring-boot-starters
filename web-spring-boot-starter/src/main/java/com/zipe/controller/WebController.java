package com.zipe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 前端視圖示範 Controller，提供 Thymeleaf 與 JSP 兩種視圖引擎的範例頁面路由。
 * <p>
 * 此 Controller 作為 web-spring-boot-starter 的示範用途，
 * 展示如何透過 Spring MVC 回傳對應的視圖名稱，由已配置的視圖解析器負責渲染。
 * </p>
 *
 * @author : Gary Tsai
 */
@Controller
@RequestMapping("/")
public class WebController {

    /**
     * 渲染 Thymeleaf 視圖的示範頁面。
     * <p>
     * 回傳視圖名稱 {@code html/hello}，由 Thymeleaf 視圖解析器對應至
     * {@code WEB-INF/html/hello.html} 並進行渲染。
     * </p>
     *
     * @return Thymeleaf 視圖名稱
     */
    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        return "html/hello";
    }

    /**
     * 渲染 JSP 視圖的示範頁面。
     * <p>
     * 回傳視圖名稱 {@code jsp/hello}，由 JSP 視圖解析器對應至
     * {@code WEB-INF/jsp/hello.jsp} 並進行渲染。
     * </p>
     *
     * @return JSP 視圖名稱
     */
    @GetMapping({"/jsp"})
    public String jsp() {
        return "jsp/hello";
    }
}
