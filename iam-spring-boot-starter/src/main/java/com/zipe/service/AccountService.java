package com.zipe.service;

import com.zipe.vo.AccountVO;
import com.zipe.vo.CreateAccountRequest;
import com.zipe.vo.UpdateAccountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 帳號管理服務，提供帳號的建立、查詢、更新、停用、密碼變更與群組指派等核心業務。
 * <p>
 * 所有方法一律回傳 {@link AccountVO} 等 DTO，不外漏 {@link com.zipe.entity.Account}
 * JPA 實體與其 lazy proxy。實作以 {@code @ConditionalOnMissingBean} 註冊，業務專案
 * 可宣告同型別 Bean 覆寫。
 * </p>
 *
 * @author Gary.Tsai
 */
public interface AccountService {

    /**
     * 建立新帳號，並以注入的密碼編碼器編碼明文密碼後儲存。
     *
     * @param request 建立帳號的請求資料
     * @return 建立完成的帳號視圖
     * @throws IllegalArgumentException 當登入帳號已存在時拋出
     */
    AccountVO createAccount(CreateAccountRequest request);

    /**
     * 依主鍵查詢單筆帳號。
     *
     * @param id 帳號主鍵
     * @return 對應的帳號視圖
     * @throws IllegalArgumentException 當帳號不存在時拋出
     */
    AccountVO getAccount(Long id);

    /**
     * 依登入帳號查詢單筆帳號。
     *
     * @param username 登入帳號
     * @return 對應的帳號視圖
     * @throws IllegalArgumentException 當帳號不存在時拋出
     */
    AccountVO getAccountByUsername(String username);

    /**
     * 分頁查詢帳號清單。
     *
     * @param pageable 分頁與排序參數
     * @return 帳號視圖的分頁結果
     */
    Page<AccountVO> listAccounts(Pageable pageable);

    /**
     * 更新帳號基本資料；請求中為 {@code null} 的欄位視為不更新。
     *
     * @param id      帳號主鍵
     * @param request 更新帳號的請求資料
     * @return 更新後的帳號視圖
     * @throws IllegalArgumentException 當帳號不存在時拋出
     */
    AccountVO updateAccount(Long id, UpdateAccountRequest request);

    /**
     * 停用帳號（將 {@code enabled} 設為 {@code false}），停用後即不可登入。
     *
     * @param id 帳號主鍵
     * @throws IllegalArgumentException 當帳號不存在時拋出
     */
    void disableAccount(Long id);

    /**
     * 變更帳號密碼，以注入的密碼編碼器編碼明文密碼後儲存。
     *
     * @param id          帳號主鍵
     * @param rawPassword 明文新密碼
     * @throws IllegalArgumentException 當帳號不存在時拋出
     */
    void changePassword(Long id, String rawPassword);

    /**
     * 將帳號加入指定群組。
     *
     * @param accountId 帳號主鍵
     * @param groupId   群組主鍵
     * @return 更新後的帳號視圖
     * @throws IllegalArgumentException 當帳號或群組不存在時拋出
     */
    AccountVO addToGroup(Long accountId, Long groupId);

    /**
     * 將帳號移出指定群組。
     *
     * @param accountId 帳號主鍵
     * @param groupId   群組主鍵
     * @return 更新後的帳號視圖
     * @throws IllegalArgumentException 當帳號或群組不存在時拋出
     */
    AccountVO removeFromGroup(Long accountId, Long groupId);
}
