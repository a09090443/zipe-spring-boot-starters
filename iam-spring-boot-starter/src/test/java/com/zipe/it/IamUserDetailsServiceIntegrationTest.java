package com.zipe.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.zipe.entity.Account;
import com.zipe.entity.Group;
import com.zipe.entity.Permission;
import com.zipe.repository.AccountRepository;
import com.zipe.repository.GroupRepository;
import com.zipe.repository.PermissionRepository;
import com.zipe.security.DbGrantedAuthoritiesResolver;
import com.zipe.security.GrantedAuthoritiesResolver;
import com.zipe.security.IamUserDetailsService;
import com.zipe.service.BasicUserServiceImpl;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

/**
 * iam-starter 整合測試：驗證引入後預設裝配的行為。
 * <p>
 * 以 {@code @SpringBootTest} 啟動最小情境（含 logon 的 {@code SecurityConfiguration} 與
 * {@code IamAutoConfiguration} 自動配置），驗證：
 * </p>
 * <ul>
 *   <li>容器中 {@code BasicUserServiceImpl} 型別的 Bean 由 iam 的 {@link IamUserDetailsService} 取代
 *       logon 寫死的預設實作；</li>
 *   <li>{@code loadUserByUsername} 能由 iam 帳號表載入 {@link UserDetails}，並含經 resolver 展開的 authorities；</li>
 *   <li>{@link GrantedAuthoritiesResolver} 預設由 iam 的 {@link DbGrantedAuthoritiesResolver} 提供
 *       （覆寫 logon 的空實作）。</li>
 * </ul>
 *
 * @author Gary.Tsai
 */
@SpringBootTest(classes = IamItApplication.class)
@Transactional
class IamUserDetailsServiceIntegrationTest {

    /** 容器中的使用者服務，預期為 iam 覆寫後的 {@link IamUserDetailsService}。 */
    @Autowired
    private BasicUserServiceImpl basicUserService;

    /** 容器中的授權解析器，預期為 iam 的 {@link DbGrantedAuthoritiesResolver}。 */
    @Autowired
    private GrantedAuthoritiesResolver authoritiesResolver;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @BeforeEach
    void seed() {
        Permission permission = new Permission();
        permission.setCode("USER_CREATE");
        permissionRepository.save(permission);

        Group group = new Group();
        group.setCode("ADMIN");
        group.setPermissions(Set.of(permission));
        groupRepository.save(group);

        Account account = new Account();
        account.setUsername("dbuser");
        account.setPassword("{noop}secret");
        account.setEnabled(Boolean.TRUE);
        account.setLocked(Boolean.FALSE);
        account.setGroups(Set.of(group));
        accountRepository.save(account);
    }

    /**
     * 驗證 iam 取代 logon 預設：容器中的 {@code basicUserServiceImpl} Bean 為
     * {@link IamUserDetailsService}，{@code grantedAuthoritiesResolver} 為
     * {@link DbGrantedAuthoritiesResolver}。
     */
    @Test
    void iamBeansOverrideLogonDefaults() {
        assertThat(basicUserService).isInstanceOf(IamUserDetailsService.class);
        assertThat(authoritiesResolver).isInstanceOf(DbGrantedAuthoritiesResolver.class);
    }

    /**
     * 驗證可由 iam 帳號表載入 {@link UserDetails}，且 authorities 已由群組／權限展開。
     */
    @Test
    void loadUserByUsernameFromIamAccountTable() {
        UserDetails userDetails = basicUserService.loadUserByUsername("dbuser");

        assertThat(userDetails.getUsername()).isEqualTo("dbuser");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "USER_CREATE");
    }
}
