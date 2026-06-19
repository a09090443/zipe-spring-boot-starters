package com.zipe.security;

import com.zipe.config.IamProperties;
import com.zipe.entity.Account;
import com.zipe.entity.Group;
import com.zipe.entity.Permission;
import com.zipe.repository.AccountRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

/**
 * 以資料庫為來源的 {@link GrantedAuthoritiesResolver} 實作。
 * <p>
 * 依帳號名稱查出帳號所屬群組與各群組擁有的權限，組成 Spring Security 的 authorities：
 * 群組 {@code code} 套用 {@code iam.group.role-prefix}（預設 {@code ROLE_}）後作為 authority，
 * 權限 {@code code} 則原樣作為 authority，兩者皆以 {@link SimpleGrantedAuthority} 表達。
 * 查無帳號時回傳空集合（而非 {@code null}），以符合 SPI 契約並保留 logon 的安全行為。
 * </p>
 * <p>
 * 此 Bean 由 {@code IamAutoConfiguration} 以 {@code @ConditionalOnMissingBean} 註冊，
 * 並以 {@link GrantedAuthoritiesResolver} 型別宣告，藉此覆寫 logon-starter 提供的預設空實作。
 * 業務專案可於容器中宣告同型別 Bean 進一步覆寫帳號→權限的對應規則。
 * </p>
 *
 * @author Gary.Tsai
 */
public class DbGrantedAuthoritiesResolver implements GrantedAuthoritiesResolver {

    /** 帳號資料存取層，供以登入名稱查詢帳號與其群組／權限。 */
    private final AccountRepository accountRepository;

    /** iam 設定屬性，用於取得群組授權前綴（role-prefix）。 */
    private final IamProperties iamProperties;

    /**
     * 建構授權解析器，注入帳號 Repository 與 iam 設定屬性。
     *
     * @param accountRepository 帳號 Repository
     * @param iamProperties     iam 設定屬性
     */
    public DbGrantedAuthoritiesResolver(AccountRepository accountRepository,
                                        IamProperties iamProperties) {
        this.accountRepository = accountRepository;
        this.iamProperties = iamProperties;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 以唯讀交易完成「帳號→群組→權限」的展開，於交易內讀取 lazy 關聯以避免
     * {@code LazyInitializationException}。查無帳號時回傳空集合。
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<? extends GrantedAuthority> resolve(String username) {
        return accountRepository.findByUsername(username)
                .map(this::toAuthorities)
                .orElseGet(Set::of);
    }

    /**
     * 將帳號展開為 authorities 集合：群組 code（套前綴）＋各群組權限 code。
     *
     * @param account 帳號實體
     * @return 對應的 authorities 集合（去重，保留插入順序）
     */
    private Set<GrantedAuthority> toAuthorities(Account account) {
        String rolePrefix = iamProperties.getGroup().getRolePrefix();
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Group group : account.getGroups()) {
            authorities.add(new SimpleGrantedAuthority(rolePrefix + group.getCode()));
            for (Permission permission : group.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
        return authorities;
    }
}
