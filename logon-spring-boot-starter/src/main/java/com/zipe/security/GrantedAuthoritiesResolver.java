package com.zipe.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 * 授權（authorities）解析擴充點（SPI）。
 *
 * <p>定義「帳號（username）→ authorities」的解析契約，作為 logon-starter
 * 與外部授權資料來源之間的整合介面。三種驗證模式（BASIC / LDAP / CUSTOM）
 * 皆可透過本介面，在認證成功後依帳號補上對應的權限集合。</p>
 *
 * <p>logon-starter 提供回傳空集合的預設實作（{@code @ConditionalOnMissingBean}），
 * 以完全保留現行行為；當業務專案或其他 starter（如 iam-starter）於容器中宣告
 * 同型別的 Bean 時，即自動覆寫預設實作，使登入帳號取得實際的群組／權限。</p>
 *
 * @author : Gary Tsai
 **/
public interface GrantedAuthoritiesResolver {

    /**
     * 依帳號名稱解析其應具備的 authorities。
     *
     * @param username 帳號名稱（已去除域名的純帳號）
     * @return 該帳號對應的 authorities 集合；查無對應時應回傳空集合而非 {@code null}
     */
    Collection<? extends GrantedAuthority> resolve(String username);
}
