package com.example.controller;

import com.zipe.security.GrantedAuthoritiesResolver;
import com.zipe.service.AccountService;
import com.zipe.vo.AccountVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * iam-spring-boot-starter 整合示範控制器。
 *
 * <p>示範兩個整合重點：</p>
 * <ol>
 *   <li>注入 iam 的 {@link AccountService} 直接管理資料庫帳號（此處示範分頁查詢）。</li>
 *   <li>注入由 iam 覆寫的 {@link GrantedAuthoritiesResolver}，展示「帳號 → 群組 → 權限」
 *       展開後的 Spring Security authorities——這正是 iam 餵給 logon 登入流程的授權來源。</li>
 * </ol>
 *
 * <p>iam 另內建 {@code /api/iam/**} 的完整 CRUD 端點；本控制器僅聚焦於展示
 * 內建 API 看不到的「授權解析結果」。示範資料請先以 {@code init/iam-demo.sql} 套用。</p>
 *
 * @author Gary Tsai
 */
@Slf4j
@RestController
@RequestMapping("/iam-demo")
public class IamDemoController {

    /** iam 帳號管理服務。 */
    private final AccountService accountService;

    /** iam 覆寫 logon 的授權解析器（DbGrantedAuthoritiesResolver）。 */
    private final GrantedAuthoritiesResolver authoritiesResolver;

    /**
     * 建構子，注入 iam 自動配置提供的服務與授權解析器。
     *
     * @param accountService      iam 帳號管理服務
     * @param authoritiesResolver iam 授權解析器
     */
    public IamDemoController(AccountService accountService,
                             GrantedAuthoritiesResolver authoritiesResolver) {
        this.accountService = accountService;
        this.authoritiesResolver = authoritiesResolver;
    }

    /**
     * 分頁查詢 iam 帳號清單。
     *
     * @param pageable 分頁與排序參數
     * @return 帳號視圖的分頁結果
     */
    @GetMapping("/accounts")
    public Page<AccountVO> accounts(Pageable pageable) {
        return accountService.listAccounts(pageable);
    }

    /**
     * 解析指定帳號的 authorities，展示群組（帶 {@code ROLE_} 前綴）與權限（原樣）的展開結果。
     *
     * @param username 登入帳號（示範資料：{@code alice} / {@code bob}）
     * @return 該帳號展開後的 authority 字串清單
     */
    @GetMapping("/authorities/{username}")
    public List<String> authorities(@PathVariable String username) {
        List<String> authorities = authoritiesResolver.resolve(username).stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        log.info("帳號 {} 解析出的 authorities：{}", username, authorities);
        return authorities;
    }
}
