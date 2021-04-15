package com.zipe.util;

import lombok.extern.slf4j.Slf4j;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 上午 11:05
 **/
@Slf4j
public class LdapUtil {
    private String username;
    private String password;
    private String ldapDomain;
    private String ldapPort;
    private String ldapDn;
    private LdapContext ctx;

    public LdapUtil(String username, String password, String ldapDomain, String ldapPort, String ldapDn) throws NamingException {
        if (null != ldapDomain && !ldapDomain.equals("")) {
            if (null != ldapPort && !ldapPort.equals("")) {
                if (null != ldapDn && !ldapDn.equals("")) {
                    this.username = username;
                    this.password = password;
                    this.ldapDomain = ldapDomain;
                    this.ldapPort = ldapPort;
                    this.ldapDn = ldapDn;
                } else {
                    throw new CommunicationException("請輸入 LDAP DN");
                }
            } else {
                throw new CommunicationException("請輸入 LDAP PORT");
            }
        } else {
            throw new CommunicationException("請輸入 LDAP Domain");
        }
    }

    public LdapContext getLdapContext() throws NamingException {
        if (null != this.username && !this.username.equals("")) {
            if (null != this.password && !this.password.equals("")) {
                try {
                    Hashtable env = new Hashtable();
                    env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
                    env.put("java.naming.security.authentication", "Simple");
                    env.put("java.naming.security.principal", this.username);
                    env.put("java.naming.security.credentials", this.password);
                    env.put("java.naming.provider.url", "ldap://" + this.ldapDomain + ":" + this.ldapPort + "/");
                    env.put("java.naming.referral", "follow");
                    log.info("LDAP 嘗試連線中...");
                    this.ctx = new InitialLdapContext(env, (Control[]) null);
                    log.info("LDAP 連線成功.");
                } catch (AuthenticationException var2) {
                    log.error("使用者帳號或密碼錯誤");
                    throw var2;
                } catch (CommunicationException var3) {
                    log.error("LDAP 連線愈時");
                    throw var3;
                } catch (NamingException var4) {
                    log.error("LDAP 連線失敗");
                    throw var4;
                }

                return this.ctx;
            } else {
                throw new AuthenticationException("請輸入使用者密碼");
            }
        } else {
            throw new AuthenticationException("請輸入使用者名稱");
        }
    }

    public Attributes loginLdap() throws NamingException {
        if (Objects.isNull(this.ctx)) {
            throw new CommunicationException("請初始 Ldap 連線!!");
        } else {
            try {
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(2);
                String[] attrIDs = new String[]{"distinguishedName", "sn", "givenname", "mail", "telephonenumber", "canonicalName", "userAccountControl", "accountExpires"};
                searchControls.setReturningAttributes(attrIDs);
                NamingEnumeration answer = this.ctx.search(this.ldapDn, "(&(objectClass=user)(sAMAccountName=" + this.username.split("@")[0] + "))", searchControls);
                if (answer.hasMoreElements()) {
                    SearchResult rslt = (SearchResult) answer.next();
                    Attributes attrs = rslt.getAttributes();
                    if (null != answer) {
                        answer.close();
                    }
                    return attrs;
                } else {
                    throw new AuthenticationException("查無使用者");
                }
            } catch (AuthenticationException var6) {
                throw var6;
            }
        }
    }

    public List<Attributes> getLdapAllUsersInfo() throws Exception {
        return this.getAllInfoByObjectClass("user");
    }

    public List<Attributes> getLdapAllGroupsInfo() throws Exception {
        return this.getAllInfoByObjectClass("group");
    }

    public List<Attributes> getAllInfoByObjectClass(String objectClass) throws Exception {
        if (Objects.isNull(this.ctx)) {
            throw new Exception("請初始 Ldap 連線!!");
        } else {
            ArrayList attributesList = new ArrayList();

            try {
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(2);
                int pageSize = 10;
                Control[] ctls = new Control[]{new PagedResultsControl(pageSize, true)};
                this.ctx.setRequestControls(ctls);
                int totalResults = 0;

                byte[] cookie;
                do {
                    for (NamingEnumeration results = this.ctx.search(this.ldapDn, "(&(objectClass=" + objectClass + "))", searchControls); results != null && results.hasMoreElements(); ++totalResults) {
                        SearchResult sr = (SearchResult) results.next();
                        attributesList.add(sr.getAttributes());
                    }

                    cookie = this.parseControls(this.ctx.getResponseControls());
                    this.ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, true)});
                } while (cookie != null && cookie.length != 0);

                log.info("Total entries: " + totalResults);
                return attributesList;
            } catch (Exception var13) {
                throw var13;
            } finally {
                this.closeConnection();
            }
        }
    }

    public void closeConnection() {
        if (!Objects.isNull(this.ctx)) {
            try {
                this.ctx.close();
            } catch (NamingException var2) {
                log.error("Ldap connection could not close");
            }
        }

    }

    private byte[] parseControls(Control[] controls) {
        byte[] cookie = null;
        if (controls != null) {
            for (int i = 0; i < controls.length; ++i) {
                if (controls[i] instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
                    cookie = prrc.getCookie();
                }
            }
        }

        return cookie == null ? new byte[0] : cookie;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
