package com.zipe.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.zipe.config.IamProperties;
import com.zipe.entity.Account;
import com.zipe.entity.Group;
import com.zipe.entity.Permission;
import com.zipe.repository.AccountRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * {@link DbGrantedAuthoritiesResolver} 的單元測試。
 * <p>
 * 以 Mockito 模擬 {@link AccountRepository}，在不連資料庫的情況下驗證
 * 「帳號→群組→權限」展開成 authorities 的規則：群組 {@code code} 套用 role-prefix、
 * 權限 {@code code} 原樣輸出，並驗證去重與查無帳號時回傳空集合的契約。
 * </p>
 *
 * @author Gary.Tsai
 */
@ExtendWith(MockitoExtension.class)
class DbGrantedAuthoritiesResolverTest {

    /** 被模擬的帳號 Repository。 */
    @Mock
    private AccountRepository accountRepository;

    /** iam 設定屬性，採預設值（role-prefix 為 {@code ROLE_}）。 */
    private IamProperties iamProperties;

    /** 受測的解析器。 */
    private DbGrantedAuthoritiesResolver resolver;

    @BeforeEach
    void setUp() {
        iamProperties = new IamProperties();
        resolver = new DbGrantedAuthoritiesResolver(accountRepository, iamProperties);
    }

    /**
     * 給定帳號→群組→權限資料，驗證 resolve 正確展開：
     * 群組帶 {@code ROLE_} 前綴，權限原樣，皆轉為 authority。
     */
    @Test
    void resolveExpandsGroupsAndPermissions() {
        Permission create = permission("USER_CREATE");
        Permission delete = permission("USER_DELETE");
        Group admin = group("ADMIN", Set.of(create, delete));
        Account account = account("alice", Set.of(admin));
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.of(account));

        var authorities = resolver.resolve("alice");

        assertThat(AuthorityUtils.authorityListToSet(
                authorities.stream().map(GrantedAuthority.class::cast).toList()))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "USER_CREATE", "USER_DELETE");
    }

    /**
     * 自訂 role-prefix 時，群組授權應套用新的前綴，權限不受影響。
     */
    @Test
    void resolveAppliesCustomRolePrefix() {
        iamProperties.getGroup().setRolePrefix("GROUP_");
        Group staff = group("STAFF", Set.of(permission("REPORT_VIEW")));
        Account account = account("bob", Set.of(staff));
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(account));

        var authorities = resolver.resolve("bob");

        assertThat(authorities.stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("GROUP_STAFF", "REPORT_VIEW");
    }

    /**
     * 多群組共用同一權限時，authorities 應去重而不重複出現。
     */
    @Test
    void resolveDeduplicatesSharedPermission() {
        Permission shared = permission("COMMON");
        Group g1 = group("G1", Set.of(shared));
        Group g2 = group("G2", Set.of(shared));
        Account account = account("carol", Set.of(g1, g2));
        when(accountRepository.findByUsername("carol")).thenReturn(Optional.of(account));

        var authorities = resolver.resolve("carol");

        assertThat(authorities.stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_G1", "ROLE_G2", "COMMON");
    }

    /**
     * 查無帳號時應回傳空集合（而非 {@code null}），以符合 SPI 契約。
     */
    @Test
    void resolveReturnsEmptyWhenAccountAbsent() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        var authorities = resolver.resolve("ghost");

        assertThat(authorities).isNotNull().isEmpty();
    }

    private Permission permission(String code) {
        Permission permission = new Permission();
        permission.setCode(code);
        return permission;
    }

    private Group group(String code, Set<Permission> permissions) {
        Group group = new Group();
        group.setCode(code);
        group.setPermissions(permissions);
        return group;
    }

    private Account account(String username, Set<Group> groups) {
        Account account = new Account();
        account.setUsername(username);
        account.setGroups(groups);
        return account;
    }
}
