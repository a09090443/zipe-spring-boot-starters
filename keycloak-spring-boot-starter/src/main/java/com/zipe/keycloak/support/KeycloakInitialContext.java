package com.zipe.keycloak.support;

import javax.naming.CompositeName;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Optional;

/**
 * Keycloak 嵌入式服務專用的 JNDI InitialContext 實作。
 *
 * <p>繼承 {@link InitialContext}，並覆寫 {@code lookup} 行為，
 * 使其直接從建構時傳入的環境表（{@code environment}）中查找命名物件，
 * 而不依賴外部 JNDI 提供者（如 LDAP、RMI 等）。
 * 這讓嵌入式 Keycloak 能在 Spring Boot 應用中正常解析所需的 JNDI 資源。</p>
 */
public class KeycloakInitialContext extends InitialContext {

    /** 儲存 JNDI 環境變數的對應表，作為命名物件的唯一查找來源。 */
    private final Hashtable<?, ?> environment;

    /**
     * 以指定的環境變數表建立 {@code KeycloakInitialContext} 實例。
     *
     * @param environment JNDI 環境變數表，包含可供查找的命名物件
     * @throws NamingException 若父類別初始化失敗時拋出
     */
    public KeycloakInitialContext(Hashtable<?, ?> environment) throws NamingException {
        super(environment);
        this.environment = environment;
    }

    /**
     * 以 {@link Name} 物件查找命名物件，委派給字串形式的 {@code lookup} 方法處理。
     *
     * @param name 要查找的 JNDI 名稱
     * @return 對應的命名物件
     * @throws NamingException 若名稱不存在時拋出
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        // 將 Name 物件轉為字串，統一由字串版 lookup 處理
        return lookup(name.toString());
    }

    /**
     * 以字串名稱查找對應的物件。
     *
     * <p>直接從 {@code environment} 環境表中取值；若不存在，
     * 則拋出 {@link NamingException} 並附上未找到的名稱資訊。</p>
     *
     * @param name 要查找的 JNDI 名稱字串
     * @return 對應的命名物件
     * @throws NamingException 若指定名稱在環境表中不存在時拋出
     */
    @Override
    public Object lookup(String name) throws NamingException {
        // 使用 Optional 包裝避免 null 直接回傳，確保找不到時能給出明確的錯誤訊息
        return Optional.ofNullable(environment.get(name))
                .orElseThrow(() -> new NamingException("Name " + name + " not found"));
    }

    /**
     * 取得指定名稱對應的 {@link NameParser}。
     *
     * <p>回傳以 {@link CompositeName} 建構子作為 lambda 的 NameParser，
     * 可將字串名稱解析為 JNDI 複合名稱物件。</p>
     *
     * @param name 名稱字串（本實作中未使用，僅符合介面規範）
     * @return 以 {@code CompositeName::new} 實作的 {@link NameParser}
     */
    @Override
    public NameParser getNameParser(String name) {
        return CompositeName::new;
    }
}
