package com.zipe.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
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
import lombok.extern.slf4j.Slf4j;

/**
 * LDAP 連線與查詢工具類別。
 *
 * <p>封裝 JNDI LDAP 連線建立、使用者驗證、以及以分頁方式批次查詢
 * 使用者（user）或群組（group）等物件資訊的常用操作。
 * 呼叫方須先透過建構子提供連線參數，再依需求呼叫
 * {@link #getLdapContext()} 建立連線後使用其他查詢方法。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/20 上午 11:05
 **/
@Slf4j
public class LdapUtil {
    /** 用於登入 LDAP 的使用者帳號（可包含 UPN 格式，如 user@domain） */
    private String username;
    /** 用於登入 LDAP 的使用者密碼 */
    private String password;
    /** LDAP 伺服器網域名稱或 IP 位址 */
    private String ldapDomain;
    /** LDAP 伺服器連接埠（通常為 389 或 636） */
    private String ldapPort;
    /** 搜尋的根識別名稱（Distinguished Name），例如 DC=example,DC=com */
    private String ldapDn;
    /** 與 LDAP 伺服器建立的連線 Context，未呼叫 getLdapContext() 前為 null */
    private LdapContext ctx;

    /**
     * 初始化 LdapUtil 並驗證必要的連線參數。
     *
     * <p>三個連線參數（ldapDomain、ldapPort、ldapDn）均不可為 null 或空字串，
     * 否則將拋出 {@link CommunicationException}。
     * 帳號與密碼的驗證延遲至 {@link #getLdapContext()} 呼叫時才進行。</p>
     *
     * @param username    登入 LDAP 的使用者帳號
     * @param password    登入 LDAP 的使用者密碼
     * @param ldapDomain  LDAP 伺服器網域名稱或 IP
     * @param ldapPort    LDAP 服務的連接埠號
     * @param ldapDn      搜尋起始的 Distinguished Name
     * @throws CommunicationException 當 ldapDomain、ldapPort 或 ldapDn 為空時拋出
     * @throws NamingException        其他 JNDI 命名例外
     */
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

    /**
     * 使用建構子提供的帳號密碼向 LDAP 伺服器建立連線，並回傳 {@link LdapContext}。
     *
     * <p>連線成功後，{@code ctx} 欄位會被設定，後續的查詢方法可直接使用。
     * 同一個 LdapUtil 實例可重複呼叫此方法以重新連線。</p>
     *
     * @return 已連線的 {@link LdapContext} 實例
     * @throws AuthenticationException 當帳號或密碼為空，或 LDAP 伺服器拒絕認證時拋出
     * @throws CommunicationException  當 LDAP 伺服器無法連線（逾時等網路問題）時拋出
     * @throws NamingException         其他 JNDI 命名例外
     */
    public LdapContext getLdapContext() throws NamingException {
        if (null != this.username && !this.username.equals("")) {
            if (null != this.password && !this.password.equals("")) {
                try {
                    // 組裝 JNDI 環境參數：使用 Simple 驗證方式並指定 LDAP 伺服器 URL
                    Hashtable env = new Hashtable();
                    env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
                    env.put("java.naming.security.authentication", "Simple");
                    env.put("java.naming.security.principal", this.username);
                    env.put("java.naming.security.credentials", this.password);
                    env.put("java.naming.provider.url", "ldap://" + this.ldapDomain + ":" + this.ldapPort + "/");
                    // follow 表示自動追蹤 LDAP referral，避免跨網域查詢中斷
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

    /**
     * 以目前的帳號在 LDAP 中搜尋對應的使用者，並回傳其屬性集合。
     *
     * <p>以 {@code sAMAccountName} 作為搜尋條件（取 username 中 {@code @} 符號前的部分），
     * 限定 {@code objectClass=user}，並回傳以下屬性：
     * distinguishedName、sn、givenname、mail、telephonenumber、
     * canonicalName、userAccountControl、accountExpires。</p>
     *
     * @return 符合條件之使用者的 {@link Attributes} 屬性集合
     * @throws CommunicationException  當尚未呼叫 {@link #getLdapContext()} 建立連線時拋出
     * @throws AuthenticationException 當 LDAP 中查無該使用者時拋出
     * @throws NamingException         其他 JNDI 命名例外
     */
    public Attributes loginLdap() throws NamingException {
        if (Objects.isNull(this.ctx)) {
            throw new CommunicationException("請初始 Ldap 連線!!");
        } else {
            try {
                SearchControls searchControls = new SearchControls();
                // SUBTREE_SCOPE (2)：從 ldapDn 起往下遞迴搜尋整棵樹
                searchControls.setSearchScope(2);
                // 僅回傳需要的屬性欄位，減少網路傳輸量
                String[] attrIDs = new String[]{"distinguishedName", "sn", "givenname", "mail", "telephonenumber", "canonicalName", "userAccountControl", "accountExpires"};
                searchControls.setReturningAttributes(attrIDs);
                // 取 UPN 中 @ 之前的部分作為 sAMAccountName 進行查詢
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

    /**
     * 取得 LDAP 中所有 objectClass 為 {@code user} 的物件屬性清單。
     *
     * @return 包含所有使用者屬性的 {@link List}，每個元素為一位使用者的 {@link Attributes}
     * @throws Exception 當連線未初始化或查詢發生錯誤時拋出
     */
    public List<Attributes> getLdapAllUsersInfo() throws Exception {
        return this.getAllInfoByObjectClass("user");
    }

    /**
     * 取得 LDAP 中所有 objectClass 為 {@code group} 的物件屬性清單。
     *
     * @return 包含所有群組屬性的 {@link List}，每個元素為一個群組的 {@link Attributes}
     * @throws Exception 當連線未初始化或查詢發生錯誤時拋出
     */
    public List<Attributes> getLdapAllGroupsInfo() throws Exception {
        return this.getAllInfoByObjectClass("group");
    }

    /**
     * 以分頁方式查詢指定 objectClass 的所有 LDAP 物件屬性。
     *
     * <p>使用 {@link PagedResultsControl} 進行分頁，每頁 10 筆，
     * 持續透過 cookie 取得下一頁，直到 cookie 為空為止，
     * 以避免一次回傳過多資料造成記憶體或伺服器負擔。
     * 查詢結束後不論成功或失敗均會呼叫 {@link #closeConnection()} 關閉連線。</p>
     *
     * @param objectClass 要查詢的 LDAP objectClass 名稱（例如 "user" 或 "group"）
     * @return 所有符合條件物件的 {@link Attributes} 清單
     * @throws Exception 當連線未初始化或分頁查詢發生錯誤時拋出
     */
    public List<Attributes> getAllInfoByObjectClass(String objectClass) throws Exception {
        if (Objects.isNull(this.ctx)) {
            throw new Exception("請初始 Ldap 連線!!");
        } else {
            ArrayList attributesList = new ArrayList();

            try {
                SearchControls searchControls = new SearchControls();
                // SUBTREE_SCOPE (2)：從 ldapDn 起往下遞迴搜尋整棵樹
                searchControls.setSearchScope(2);
                // 每頁查詢筆數，避免一次拉取大量資料造成效能問題
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

                    // 從回應控制項中解析分頁 cookie，用以判斷是否還有下一頁
                    cookie = this.parseControls(this.ctx.getResponseControls());
                    this.ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, true)});
                } while (cookie != null && cookie.length != 0);

                log.info("Total entries: " + totalResults);
                return attributesList;
            } catch (Exception var13) {
                throw var13;
            } finally {
                // 無論查詢成功或失敗，均確保關閉 LDAP 連線以釋放資源
                this.closeConnection();
            }
        }
    }

    /**
     * 關閉與 LDAP 伺服器的連線並釋放資源。
     *
     * <p>若 {@code ctx} 尚未初始化則不執行任何動作；
     * 關閉失敗時僅記錄錯誤日誌，不向外拋出例外。</p>
     */
    public void closeConnection() {
        if (!Objects.isNull(this.ctx)) {
            try {
                this.ctx.close();
            } catch (NamingException var2) {
                log.error("Ldap connection could not close");
            }
        }

    }

    /**
     * 從 LDAP 回應控制項陣列中解析分頁結果的 cookie。
     *
     * <p>找到 {@link PagedResultsResponseControl} 後取出其 cookie 值；
     * 若控制項陣列為 null 或未包含分頁控制項，則回傳長度為 0 的空陣列，
     * 讓呼叫端以 {@code cookie.length != 0} 判斷是否還有下一頁。</p>
     *
     * @param controls LDAP 伺服器回傳的控制項陣列，可為 null
     * @return 分頁 cookie 位元組陣列；若無分頁控制項則回傳長度為 0 的陣列
     */
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

        // cookie 為 null 表示伺服器未回傳分頁控制項，以空陣列代替，統一以 length==0 判斷結束
        return cookie == null ? new byte[0] : cookie;
    }

    /**
     * 設定用於 LDAP 連線的使用者帳號。
     *
     * @param username 使用者帳號
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 設定用於 LDAP 連線的使用者密碼。
     *
     * @param password 使用者密碼
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
