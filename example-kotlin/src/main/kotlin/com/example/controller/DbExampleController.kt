package com.example.controller

import com.example.model.UserMain
import com.example.service.DBExampleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 資料庫範例 RESTful 控制器，示範 db-spring-boot-starter 的多資料來源動態切換。
 *
 * 提供可由 HTTP 直接觀察切換效果的端點：傳入不同的 `ds` 參數，
 * 查詢會被路由到對應的資料來源。搭配各資料源的獨有資料
 * （example1 → `OnlyExample1`、example2 → `OnlyExample2`、postgres → `OnlyPostgres`），
 * 即可驗證切換是否真正生效。
 *
 * @author Gary
 */
@RestController
@RequestMapping("/rest/db")
class DbExampleController(
    /** 資料庫範例服務，負責切換資料來源並執行查詢。 */
    private val dbExampleService: DBExampleService,
) {

    /**
     * 切換至指定資料來源後，依使用者名稱查詢主要資料。
     *
     * 範例：`GET /rest/db/user?name=OnlyExample1&ds=example1` 查得到資料，
     * 而 `GET /rest/db/user?name=OnlyExample1&ds=example2` 因 example2 無此筆資料而回傳 404，
     * 即可從 HTTP 觀察查詢確實被路由到不同資料來源。
     *
     * @param name 欲查詢的使用者名稱
     * @param dataSourceName 目標資料來源名稱（須為 data-source.properties 中實際存在的資料源），預設 example1
     * @return 查得資料時回傳 200 與 [UserMain]；查無資料時回傳 404
     */
    @GetMapping("/user")
    fun getUser(
        @RequestParam name: String,
        @RequestParam(name = "ds", defaultValue = "example1") dataSourceName: String,
    ): ResponseEntity<UserMain> =
        ResponseEntity.ofNullable(dbExampleService.getUserMainByName(name, dataSourceName))
}
