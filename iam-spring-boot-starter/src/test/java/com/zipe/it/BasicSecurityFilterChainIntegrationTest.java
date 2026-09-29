package com.zipe.it;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zipe.entity.Account;
import com.zipe.entity.Group;
import com.zipe.entity.Permission;
import com.zipe.repository.AccountRepository;
import com.zipe.repository.GroupRepository;
import com.zipe.repository.PermissionRepository;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * iam＋logon BASIC 模式的端到端整合測試：請求經實際 {@code SecurityFilterChain}（HTTP Basic）
 * 認證，並由 {@code @PreAuthorize} 以資料庫群組展開的權限授權。
 * <p>
 * 涵蓋三條路徑：
 * </p>
 * <ul>
 *   <li>具目標權限的帳號以正確密碼登入成功，principal 帶有由群組展開的權限；</li>
 *   <li>同帳號錯誤密碼被拒（401），且未建立已認證 principal；</li>
 *   <li>可認證但缺目標權限的帳號存取受保護端點得到 403。</li>
 * </ul>
 * 帳號密碼以容器中的 {@link PasswordEncoder} 編碼後存入 iam 帳號表，驗證走真正的密碼比對。
 *
 * @author Gary.Tsai
 */
@SpringBootTest(classes = {IamItApplication.class, BasicSecurityFilterChainIntegrationTest.ProtectedController.class})
@AutoConfigureMockMvc
@Transactional
class BasicSecurityFilterChainIntegrationTest {

    /** 受保護端點路徑。 */
    private static final String PROTECTED_URI = "/it/users/create";

    /** 測試帳號共用的明文密碼（僅測試資料）。 */
    private static final String RAW_PASSWORD = "it-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @BeforeEach
    void seed() {
        Permission create = new Permission();
        create.setCode("USER_CREATE");
        permissionRepository.save(create);

        Permission read = new Permission();
        read.setCode("USER_READ");
        permissionRepository.save(read);

        Group admin = new Group();
        admin.setCode("ADMIN");
        admin.setPermissions(Set.of(create));
        groupRepository.save(admin);

        Group viewer = new Group();
        viewer.setCode("VIEWER");
        viewer.setPermissions(Set.of(read));
        groupRepository.save(viewer);

        saveAccount("dbadmin", admin);
        saveAccount("dbviewer", viewer);
    }

    /**
     * 正確帳密：回應 2xx、principal 為 dbadmin，且權限為資料庫群組展開後的結果。
     */
    @Test
    void correctPasswordWithAuthorityIsGranted() throws Exception {
        mockMvc.perform(get(PROTECTED_URI).with(httpBasic("dbadmin", RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string("dbadmin:[ROLE_ADMIN, USER_CREATE]"))
                .andExpect(authenticated().withUsername("dbadmin"));
    }

    /**
     * 錯誤密碼：回應 401，且未建立已認證 principal。
     */
    @Test
    void wrongPasswordIsRejectedWithoutPrincipal() throws Exception {
        mockMvc.perform(get(PROTECTED_URI).with(httpBasic("dbadmin", "wrong-" + RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    /**
     * 可認證但缺目標權限：回應 403（已認證，但授權被拒）。
     */
    @Test
    void authenticatedWithoutAuthorityIsForbidden() throws Exception {
        mockMvc.perform(get(PROTECTED_URI).with(httpBasic("dbviewer", RAW_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(authenticated().withUsername("dbviewer"));
    }

    private void saveAccount(String username, Group group) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        account.setEnabled(Boolean.TRUE);
        account.setLocked(Boolean.FALSE);
        account.setGroups(Set.of(group));
        accountRepository.save(account);
    }

    /**
     * 測試用受保護端點：需 {@code USER_CREATE} 權限，回傳「登入者:排序後權限」。
     * <p>
     * Spring Security 7 會在密碼認證成功時自動附加 {@code FACTOR_*} 認證因子權限（多因子機制），
     * 其與資料庫授權無關，故回傳前排除，只比對由 iam 群組／權限展開的結果。
     * </p>
     */
    @RestController
    static class ProtectedController {

        @GetMapping(PROTECTED_URI)
        @PreAuthorize("hasAuthority('USER_CREATE')")
        public String create(Authentication authentication) {
            Set<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> !authority.startsWith("FACTOR_"))
                    .collect(Collectors.toCollection(TreeSet::new));
            return authentication.getName() + ":" + authorities;
        }
    }
}
