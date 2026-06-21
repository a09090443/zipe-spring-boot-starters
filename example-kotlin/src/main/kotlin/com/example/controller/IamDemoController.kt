package com.example.controller

import com.zipe.security.GrantedAuthoritiesResolver
import com.zipe.service.AccountService
import com.zipe.vo.AccountVO
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * iam-spring-boot-starter 整合示範控制器。
 *
 * 示範兩個整合重點：
 * 1. 注入 iam 的 [AccountService] 直接管理資料庫帳號（此處示範分頁查詢）。
 * 2. 注入由 iam 覆寫的 [GrantedAuthoritiesResolver]，展示「帳號 → 群組 → 權限」
 *    展開後的 Spring Security authorities——這正是 iam 餵給 logon 登入流程的授權來源。
 * 3. 以 [PreAuthorize] 將 iam 權限套用到實際端點：示範 `ORDER_EXPORT`
 *    與 `USER_MANAGE` 兩個權限對端點的差異化保護（method security 由 logon 的
 *    `@EnableMethodSecurity` 啟用）。需先以 custom 登入（如 `user01/1234`、
 *    `user02/abcd`），登入後的使用者才會帶上 iam 授權（見 `DbAuthProvider`）。
 *
 * iam 另內建 `/api/iam/` 下的萬用 CRUD 端點；本控制器聚焦於展示
 * 內建 API 看不到的「授權解析結果」與「以權限保護端點」。示範資料請先以
 * `init/iam-demo.sql` 套用。
 *
 * @author Gary Tsai
 */
@RestController
@RequestMapping("/iam-demo")
class IamDemoController(
    /** iam 帳號管理服務。 */
    private val accountService: AccountService,
    /** iam 覆寫 logon 的授權解析器（DbGrantedAuthoritiesResolver）。 */
    private val authoritiesResolver: GrantedAuthoritiesResolver,
) {

    private val log = LoggerFactory.getLogger(IamDemoController::class.java)

    /**
     * 分頁查詢 iam 帳號清單。
     *
     * @param pageable 分頁與排序參數
     * @return 帳號視圖的分頁結果
     */
    @GetMapping("/accounts")
    fun accounts(pageable: Pageable): Page<AccountVO> = accountService.listAccounts(pageable)

    /**
     * 解析指定帳號的 authorities，展示群組（帶 `ROLE_` 前綴）與權限（原樣）的展開結果。
     *
     * @param username 登入帳號（示範資料：`alice` / `bob`）
     * @return 該帳號展開後的 authority 字串清單
     */
    @GetMapping("/authorities/{username}")
    fun authorities(@PathVariable username: String): List<String> {
        val authorities = authoritiesResolver.resolve(username)
            .mapNotNull { it.authority }
        log.info("帳號 {} 解析出的 authorities：{}", username, authorities)
        return authorities
    }

    /**
     * 受 `ORDER_EXPORT` 權限保護的端點，示範匯出訂單。
     *
     * 需登入且具備 `ORDER_EXPORT` 權限（user01、user02 皆有）；
     * 否則回應 HTTP 403。
     *
     * @return 匯出結果訊息
     */
    @GetMapping("/orders/export")
    @PreAuthorize("hasAuthority('ORDER_EXPORT')")
    fun exportOrders(): String {
        log.info("執行訂單匯出（需 ORDER_EXPORT 權限）")
        return "訂單已匯出"
    }

    /**
     * 受 `USER_MANAGE` 權限保護的端點，示範使用者管理。
     *
     * 需登入且具備 `USER_MANAGE` 權限（僅 user02／ADMIN 群組）；
     * 僅有 `ORDER_EXPORT` 的 user01 會被擋下回應 HTTP 403。
     *
     * @return 管理結果訊息
     */
    @GetMapping("/users/manage")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    fun manageUsers(): String {
        log.info("執行使用者管理（需 USER_MANAGE 權限）")
        return "已進入使用者管理"
    }
}
