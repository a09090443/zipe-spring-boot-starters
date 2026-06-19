package com.example.config

import com.example.repository.UserLoginRepository
import com.zipe.security.GrantedAuthoritiesResolver
import com.zipe.service.CommonLoginProcess
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * 業務端自訂登入（CUSTOM 模式）的 [com.zipe.service.CommonLoginProcess] 實作範例。
 *
 * 本類別示範如何在業務系統中接上自家的使用者資料庫進行帳號密碼驗證：
 * - 繼承 [CommonLoginProcess]，因此自動沿用父類別的 ADMIN 動態密碼機制
 *   （以當日日期作為密碼，無須覆寫即可使用）。
 * - 覆寫 [verifyNormalUser]，以 [UserLoginRepository]
 *   查詢 `user_login` 資料表中的帳號，並用注入的 [PasswordEncoder]
 *   比對密碼。
 * - 注入 [GrantedAuthoritiesResolver]（由 iam-starter 覆寫為 DB 版本），
 *   在帳密驗證成功後依帳號補上 iam 的群組／權限 authorities——
 *   **authn 用自家 `user_login`，authz 改由 iam 疊上**，
 *   示範認證來源與授權來源解耦。CUSTOM 模式下框架不會自動套用 resolver，
 *   須如本類別於驗證成功時自行呼叫（BASIC／JWT 模式則由 iam 的
 *   `IamUserDetailsService` 代為處理）。
 *
 * 啟用方式：將 `application.yml` 的 `security.verification-type`
 * 由 `basic` 改為 `custom`，並確認 `security.custom-bean-name`
 * 指向本 Bean 名稱 `dbAuthProvider` 即可。
 *
 * 密碼比對說明：`data.sql` 內寫入的是 BCrypt 雜湊字串（無 `{id}` 前綴），
 * 因此這裡使用 [PasswordEncoder.matches] 將使用者輸入的
 * 原始密碼與資料庫中的雜湊值比對；`logon-spring-boot-starter` 預設提供的
 * `PasswordEncoder` Bean 為委派式 `DelegatingPasswordEncoder`，已設定以
 * `BCryptPasswordEncoder` 作為無前綴雜湊的比對後援，故可直接比對既有的 `$2a$` 雜湊。
 *
 * @author Gary Tsai
 */
@Component("dbAuthProvider")
class DbAuthProvider(
    passwordEncoder: PasswordEncoder,
    /** 登入帳號資料存取介面，用於依帳號查詢密碼雜湊。 */
    private val userLoginRepository: UserLoginRepository,
    /** 授權解析器（iam-starter 覆寫為 DB 版本），依帳號解析 iam 群組／權限。 */
    private val authoritiesResolver: GrantedAuthoritiesResolver,
) : CommonLoginProcess(passwordEncoder) {

    /**
     * 以資料庫帳號驗證一般使用者的帳號與密碼。
     *
     * 流程說明：
     * 1. 依 `loginId` 查詢 `user_login` 資料表。
     * 2. 查無帳號時拋出 [BadCredentialsException]（不揭露是帳號或密碼錯誤）。
     * 3. 以 [PasswordEncoder.matches] 比對輸入密碼與儲存的 BCrypt 雜湊。
     * 4. 以 [GrantedAuthoritiesResolver] 依帳號解析 iam 群組／權限，
     *    作為已認證 token 的 authorities（查無對應時為空集合）。
     * 5. 驗證成功回傳已認證的 [UsernamePasswordAuthenticationToken]，
     *    credentials 設為 null 以避免明文密碼殘留於安全內容中。
     *
     * @param loginId  登入帳號
     * @param password 使用者輸入的原始密碼（明文）
     * @return 已認證的 [UsernamePasswordAuthenticationToken]，authorities 來自 iam
     * @throws BadCredentialsException 帳號不存在或密碼比對失敗時拋出
     */
    override fun verifyNormalUser(loginId: String, password: String): UsernamePasswordAuthenticationToken {
        val userLogin = userLoginRepository.findByLoginId(loginId)
        if (userLogin == null) {
            log.warn("使用者:{} 帳號不存在", loginId)
            throw BadCredentialsException("使用者:$loginId 帳號或密碼錯誤")
        }
        if (!passwordEncoder.matches(password, userLogin.password)) {
            log.warn("使用者:{} 密碼錯誤", loginId)
            throw BadCredentialsException("使用者:$loginId 帳號或密碼錯誤")
        }
        // 帳密由 user_login 驗證（authn），群組／權限改由 iam 解析（authz）——兩者解耦
        val authorities = authoritiesResolver.resolve(loginId)
        log.info("使用者:{} 登入成功，iam 授權:{}", loginId, authorities)
        // 帶入 iam authorities 使 token 成為已認證狀態，並清除明文密碼
        return UsernamePasswordAuthenticationToken(loginId, null, authorities)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DbAuthProvider::class.java)
    }
}
