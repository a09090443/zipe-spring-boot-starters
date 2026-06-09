package com.zipe.service;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.exception.LdapException;
import com.zipe.model.LdapUser;
import com.zipe.util.LdapUtil;
import com.zipe.util.string.StringConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.naming.AuthenticationException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Objects;

/**
 * 基於 LDAP（Active Directory）的使用者驗證服務。
 *
 * <p>繼承 {@link CommonLoginProcess}，實作透過 LDAP 目錄服務進行帳號密碼驗證的流程。
 * 驗證成功後，會從 AD 取出使用者屬性（如 distinguishedName、email），
 * 並回傳已認證的 {@code UsernamePasswordAuthenticationToken}。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 下午 05:30
 **/
@Slf4j
public class LdapUserDetailsService extends CommonLoginProcess {

    /** 讀取 LDAP 連線參數（IP、埠號、DN、Domain）所需的設定屬性 */
    private final SecurityPropertyConfig securityPropertyConfig;

    /**
     * 建構 {@code LdapUserDetailsService}，注入密碼編碼器與 Security 設定。
     *
     * @param passwordEncoder        Spring Security 密碼編碼器，傳遞給父類別使用
     * @param securityPropertyConfig 包含 LDAP 連線參數的設定屬性物件
     */
    public LdapUserDetailsService(PasswordEncoder passwordEncoder, SecurityPropertyConfig securityPropertyConfig) {
        super(passwordEncoder);
        this.securityPropertyConfig = securityPropertyConfig;
    }

    /**
     * 以 LDAP（Active Directory）驗證一般使用者的帳號與密碼。
     *
     * <p>流程說明：</p>
     * <ol>
     *   <li>若帳號未含域名（{@code @domain}），自動補上設定中的 Domain。</li>
     *   <li>透過 {@link LdapUtil} 建立 LDAP 連線並取得使用者屬性。</li>
     *   <li>將 LDAP 屬性轉換為 {@link LdapUser}。</li>
     *   <li>驗證成功後移除域名部分，回傳已認證的 token。</li>
     * </ol>
     *
     * @param loginId  使用者登入帳號（可含或不含 {@code @domain}）
     * @param password 使用者密碼（明文）
     * @return 已認證的 {@code UsernamePasswordAuthenticationToken}，credentials 為 null
     * @throws BadCredentialsException 帳號或密碼錯誤時拋出
     * @throws LdapException           LDAP 伺服器無回應時拋出
     */
    @Override
    public UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password) {
        LdapUtil ldapUtil = null;
        String ldapIp = securityPropertyConfig.getLdap().getIp();
        String domain = securityPropertyConfig.getLdap().getDomain();
        String port = securityPropertyConfig.getLdap().getPort();
        String dn = securityPropertyConfig.getLdap().getDn();
        // 帳號如無域名自動加入
        String fullLoginId = loginId.split(StringConstant.AT).length > 1 ? loginId : loginId + StringConstant.AT + domain;
        Attributes attrs;
        LdapContext ctx;
        LdapUser ldapUser;
        try {
            ldapUtil = new LdapUtil(fullLoginId, password, ldapIp, port, dn);
            ctx = ldapUtil.getLdapContext();
            attrs = ldapUtil.loginLdap();
            ldapUser = convertLdapUser(loginId, attrs, ctx);
        } catch (AuthenticationException ae) {
            log.warn("使用者:{} 帳號或密碼錯誤", loginId);
            throw new BadCredentialsException("使用者:" + loginId + " 帳號或密碼錯誤");
        } catch (NamingException ne) {
            log.warn("帳號系統連線無回應");
            throw new LdapException("帳號系統連線無回應");
        } catch (Exception e) {
            log.warn("使用者:{} 登入失敗, 原因為:{}", loginId, e.getMessage());
            throw new BadCredentialsException(e.getMessage());
        } finally {
            if (null != ldapUtil) {
                ldapUtil.closeConnection();
            }
        }
        log.info("使用者:{} 登入成功", loginId);

        // 如使用 AD 帳號登入時需要域名需將域名移除
        String[] userNameSplit = fullLoginId.split(StringConstant.AT);
        return buildAuthenticatedToken(userNameSplit[0]);
    }

    /**
     * 建立認證成功的 Authentication token。
     *
     * <p>使用非 null 的權限集合使 token 成為已認證狀態，且不在 token 內保留
     * 明文密碼（credentials 設為 null），避免敏感資訊殘留於安全內容中。</p>
     *
     * @param principal 使用者主體（已移除域名的帳號）
     * @return 已認證的 token
     */
    static UsernamePasswordAuthenticationToken buildAuthenticatedToken(String principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    /**
     * 將 LDAP 連線環境資訊與屬性集合轉換為 {@link LdapUser} 物件。
     *
     * <p>從 {@code context} 的環境變數中取出登入主體作為 email，
     * 並從 {@code attrs} 中解析 {@code distinguishedName} 作為 LDAP DN。</p>
     *
     * @param userId  使用者登入帳號（原始輸入，不含域名）
     * @param attrs   LDAP 查詢結果的屬性集合
     * @param context 已驗證的 LDAP 目錄內容物件
     * @return 填入基本資料的 {@link LdapUser} 實例
     * @throws Exception 取得環境變數或屬性時若發生命名服務錯誤則拋出
     */
    private LdapUser convertLdapUser(String userId, Attributes attrs, DirContext context) throws Exception {
        Hashtable<?, ?> envInfo = context.getEnvironment();
        LdapUser ldapUser = new LdapUser();
        ldapUser.setUserId(userId);
        ldapUser.setIsEnabled(StringConstant.SHORT_YES);
        ldapUser.setEmail(envInfo.get("java.naming.security.principal").toString());
        ldapUser.setLdapDn(getAttrValue(attrs, "distinguishedName"));
        return ldapUser;
    }


    /**
     * 依 LDAP 欄位屬性名稱取出對應的純值字串。
     *
     * <p>LDAP 屬性的字串表示格式為 {@code attrName: value}，例如：
     * {@code distinguishedName: CN=PWA User,OU=System,OU=zipe,DC=com,DC=tw}。
     * 本方法以第一個冒號為分隔符切割，只取冒號後的內容值並去除前後空白。
     * 若屬性不存在則回傳 {@code null}。</p>
     *
     * @param attrs    LDAP 查詢結果的屬性集合
     * @param attrName 要取出的屬性名稱（例如 {@code "distinguishedName"}）
     * @return 屬性的純值字串，若屬性不存在則為 {@code null}
     */
    private String getAttrValue(Attributes attrs, String attrName) {
        return !Objects.isNull(attrs.get(attrName)) ? attrs.get(attrName).toString().split(StringConstant.COLON, 2)[1].trim() : null;
    }
}
