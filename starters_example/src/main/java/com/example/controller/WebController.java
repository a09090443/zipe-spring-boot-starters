package com.example.controller;

import com.zipe.base.controller.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Web 頁面控制器，負責處理前端頁面的路由請求。
 * <p>
 * 示範如何透過 Spring MVC 分別回傳 Thymeleaf 模板、JSP 頁面以及首頁視圖，
 * 整合 {@link BaseController} 所提供的通用頁面初始化機制。
 * </p>
 *
 * @author Zipe
 */
@Controller
@RequestMapping("/")
public class WebController extends BaseController {

    /**
     * 回傳 Thymeleaf 模板頁面。
     *
     * @return Thymeleaf 模板的邏輯視圖名稱（對應 templates/html/hello.html）
     */
    @GetMapping({"/thymeleaf"})
    public String thymeleaf() {
        return "html/hello";
    }

    /**
     * 回傳 JSP 頁面。
     *
     * @return JSP 的邏輯視圖名稱（對應 WEB-INF/jsp/hello.jsp）
     */
    @GetMapping({"/jsp"})
    public String jsp() {
        return "jsp/hello";
    }

    /**
     * 回傳展示用的 demo 頁面。
     *
     * @return Thymeleaf 模板的邏輯視圖名稱（對應 templates/html/demo.html）
     */
    @GetMapping({"/demo"})
    public String demo() {
        return "html/demo";
    }

    /**
     * 初始化首頁視圖，並注入預設顯示名稱。
     * <p>
     * 覆寫 {@link BaseController#initPage()} 以提供首頁（index）所需的 Model 資料。
     * </p>
     *
     * @return 包含 {@code name} 屬性的 {@link ModelAndView}，視圖指向 index 頁面
     */
    @Override
    @GetMapping({"/"})
    public ModelAndView initPage() {
        ModelAndView view = new ModelAndView("index");
        // 將顯示名稱注入 Model，供 index 模板渲染使用
        view.addObject("name", "John");
        return view;
    }
}
