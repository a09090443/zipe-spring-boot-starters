package com.zipe.controller;

import com.zipe.service.AccountService;
import com.zipe.vo.AccountVO;
import com.zipe.vo.CreateAccountRequest;
import com.zipe.vo.UpdateAccountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帳號管理的內建 REST Controller，委派 {@link AccountService} 提供帳號 CRUD、密碼變更與群組指派等端點。
 * <p>
 * 路由前綴採用屬性佔位符 {@code ${iam.api.base-path:/api/iam}}，於應用程式啟動時由
 * {@link com.zipe.config.IamProperties} 的 {@code iam.api.base-path} 解析（預設 {@code /api/iam}），
 * 因此本 Controller 的端點預設位於 {@code /api/iam/accounts} 之下。
 * </p>
 * <p>
 * 本 Controller 為可選成品：以 {@code @ConditionalOnProperty(prefix="iam.api", name="enabled")}
 * 控制是否裝配，並於 {@link com.zipe.autoconfiguration.IamAutoConfiguration} 內以
 * {@code @ConditionalOnMissingBean} 註冊，業務專案可宣告同型別 Bean 覆寫，或以
 * {@code iam.api.enabled=false} 完全關閉自行實作。
 * </p>
 *
 * @author Gary.Tsai
 */
@RestController
@RequestMapping("${iam.api.base-path:/api/iam}/accounts")
public class AccountController {

    private final AccountService accountService;

    /**
     * 建立帳號 Controller。
     *
     * @param accountService 帳號管理服務
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 建立新帳號。
     *
     * @param request 建立帳號的請求資料
     * @return 建立完成的帳號視圖
     */
    @PostMapping
    public ResponseEntity<AccountVO> create(@RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    /**
     * 依主鍵查詢單筆帳號。
     *
     * @param id 帳號主鍵
     * @return 對應的帳號視圖
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountVO> get(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    /**
     * 分頁查詢帳號清單。
     *
     * @param pageable 分頁與排序參數
     * @return 帳號視圖的分頁結果
     */
    @GetMapping
    public ResponseEntity<Page<AccountVO>> list(Pageable pageable) {
        return ResponseEntity.ok(accountService.listAccounts(pageable));
    }

    /**
     * 更新帳號基本資料；請求中為 {@code null} 的欄位視為不更新。
     *
     * @param id      帳號主鍵
     * @param request 更新帳號的請求資料
     * @return 更新後的帳號視圖
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountVO> update(@PathVariable Long id,
                                            @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    /**
     * 停用帳號（將 {@code enabled} 設為 {@code false}）。
     *
     * @param id 帳號主鍵
     * @return 無內容回應
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disable(@PathVariable Long id) {
        accountService.disableAccount(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 變更帳號密碼，明文密碼將由服務層編碼後儲存。
     *
     * @param id          帳號主鍵
     * @param rawPassword 明文新密碼（以 request body 純字串傳入）
     * @return 無內容回應
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @RequestBody String rawPassword) {
        accountService.changePassword(id, rawPassword);
        return ResponseEntity.noContent().build();
    }

    /**
     * 將帳號加入指定群組。
     *
     * @param id      帳號主鍵
     * @param groupId 群組主鍵
     * @return 更新後的帳號視圖
     */
    @PostMapping("/{id}/groups/{groupId}")
    public ResponseEntity<AccountVO> addToGroup(@PathVariable Long id,
                                                @PathVariable Long groupId) {
        return ResponseEntity.ok(accountService.addToGroup(id, groupId));
    }

    /**
     * 將帳號移出指定群組。
     *
     * @param id      帳號主鍵
     * @param groupId 群組主鍵
     * @return 更新後的帳號視圖
     */
    @DeleteMapping("/{id}/groups/{groupId}")
    public ResponseEntity<AccountVO> removeFromGroup(@PathVariable Long id,
                                                     @PathVariable Long groupId) {
        return ResponseEntity.ok(accountService.removeFromGroup(id, groupId));
    }
}
