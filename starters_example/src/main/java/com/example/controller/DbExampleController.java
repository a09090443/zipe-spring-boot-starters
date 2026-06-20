package com.example.controller;

import com.example.model.UserMain;
import com.example.service.DBExampleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 資料庫範例 RESTful 控制器，示範 db-spring-boot-starter 的多資料來源動態切換。
 *
 * <p>提供可由 HTTP 直接觀察切換效果的端點：傳入不同的 {@code ds} 參數，
 * 查詢會被路由到對應的資料來源。搭配各資料源的獨有資料
 * （example1 → {@code OnlyExample1}、example2 → {@code OnlyExample2}、
 * postgres → {@code OnlyPostgres}），即可驗證切換是否真正生效。</p>
 *
 * @author Gary
 */
@Slf4j
@RestController
@RequestMapping("/rest/db")
public class DbExampleController {

    /** 資料庫範例服務，負責切換資料來源並執行查詢。 */
    private final DBExampleService dbExampleService;

    /**
     * 建構子，透過 Spring 依賴注入初始化 {@link DBExampleService}。
     *
     * @param dbExampleService 資料庫範例服務實例
     */
    @Autowired
    public DbExampleController(DBExampleService dbExampleService) {
        this.dbExampleService = dbExampleService;
    }

    /**
     * 切換至指定資料來源後，依使用者名稱查詢主要資料。
     *
     * <p>範例：{@code GET /rest/db/user?name=OnlyExample1&ds=example1} 查得到資料，
     * 而 {@code GET /rest/db/user?name=OnlyExample1&ds=example2} 因 example2 無此筆資料而回傳 404，
     * 即可從 HTTP 觀察查詢確實被路由到不同資料來源。</p>
     *
     * @param name           欲查詢的使用者名稱
     * @param dataSourceName 目標資料來源名稱（須為 data-source.properties 中實際存在的資料源），預設 example1
     * @return 查得資料時回傳 200 與 {@link UserMain}；查無資料時回傳 404
     */
    @GetMapping("/user")
    public ResponseEntity<UserMain> getUser(
            @RequestParam String name,
            @RequestParam(name = "ds", defaultValue = "example1") String dataSourceName) {
        log.debug("查詢 name={}，指定資料來源 ds={}", name, dataSourceName);
        UserMain userMain = dbExampleService.getUserMainByName(name, dataSourceName);
        return ResponseEntity.ofNullable(userMain);
    }
}
