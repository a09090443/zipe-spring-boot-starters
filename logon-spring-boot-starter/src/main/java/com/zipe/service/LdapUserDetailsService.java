package com.zipe.service;

import com.zipe.config.LdapPropertyConfig;
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
import java.util.Hashtable;
import java.util.Objects;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 下午 05:30
 **/
@Slf4j
public class LdapUserDetailsService extends CommonLoginProcess {

    private LdapPropertyConfig ldapPropertyConfig;

    public LdapUserDetailsService(PasswordEncoder passwordEncoder, LdapPropertyConfig ldapPropertyConfig) {
        super(passwordEncoder);
        this.ldapPropertyConfig = ldapPropertyConfig;
    }

    /**
     * 一般使用者認證程序
     *
     * @param loginId
     * @param password
     * @return
     */
    @Override
    public UsernamePasswordAuthenticationToken verifyNormalUser(String loginId, String password) {
        LdapUtil ldapUtil = null;
        String ldapIp = ldapPropertyConfig.getIp();
        String domain = ldapPropertyConfig.getDomain();
        String port = ldapPropertyConfig.getPort();
        String dn = ldapPropertyConfig.getDn();
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
        return new UsernamePasswordAuthenticationToken(userNameSplit[0], password, null);
    }

    private LdapUser convertLdapUser(String userId, Attributes attrs, DirContext context) throws Exception {
        Hashtable envInfo = context.getEnvironment();
        LdapUser ldapUser = new LdapUser();
        ldapUser.setUserId(userId);
        ldapUser.setIsEnabled(StringConstant.SHORT_YES);
        ldapUser.setEmail(envInfo.get("java.naming.security.principal").toString());
        ldapUser.setLdapDn(getAttrValue(attrs, "distinguishedName"));
        return ldapUser;
    }


    /**
     * 依LDAP的欄位屬性名稱取出，如: distinguishedName: CN=PWA User
     * PWA使用者,OU=System,OU=zipe,DC=com,DC=tw 並分割字串並只取得內容值
     *
     * @param attrs
     * @param attrName
     * @return
     */
    private String getAttrValue(Attributes attrs, String attrName) {
        return !Objects.isNull(attrs.get(attrName)) ? attrs.get(attrName).toString().split(StringConstant.COLON, 2)[1].trim() : null;
    }
}
