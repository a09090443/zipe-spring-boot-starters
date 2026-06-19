package com.zipe.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.zipe.entity.Account;
import com.zipe.entity.Group;
import com.zipe.entity.Permission;
import com.zipe.it.IamItApplication;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帳號／群組／權限 Repository 層的持久化測試。
 * <p>
 * 以 {@code @SpringBootTest} 搭配記憶體 H2 驗證 CRUD 與多對多關聯查詢：
 * 帳號以 {@code findByUsername} 取得，並可沿關聯讀出所屬群組與群組擁有的權限。
 * 由 Hibernate {@code ddl-auto=create-drop} 依實體標註自動建表（僅測試範圍），無須額外建表腳本；
 * 各測試以 {@code @Transactional} 回滾，彼此隔離。
 * </p>
 *
 * @author Gary.Tsai
 */
@SpringBootTest(classes = IamItApplication.class)
@Transactional
class AccountRepositoryTest {

    /** 帳號 Repository（受測對象）。 */
    @Autowired
    private AccountRepository accountRepository;

    /** 群組 Repository。 */
    @Autowired
    private GroupRepository groupRepository;

    /** 權限 Repository。 */
    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * 驗證帳號的新增與依登入名稱查詢：存入後可由 {@code findByUsername} 取回相同資料。
     */
    @Test
    void findByUsernameReturnsSavedAccount() {
        Account account = new Account();
        account.setUsername("alice");
        account.setPassword("hashed");
        account.setDisplayName("Alice");
        accountRepository.save(account);

        Optional<Account> found = accountRepository.findByUsername("alice");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Alice");
        assertThat(found.get().getEnabled()).isTrue();
        assertThat(found.get().getLocked()).isFalse();
    }

    /**
     * 查無對應登入名稱時，{@code findByUsername} 應回傳空的 {@link Optional}。
     */
    @Test
    void findByUsernameReturnsEmptyWhenAbsent() {
        assertThat(accountRepository.findByUsername("nobody")).isEmpty();
    }

    /**
     * 驗證「帳號→群組→權限」多對多關聯查詢：
     * 帳號加入群組、群組掛上權限後，由帳號可沿關聯展開到群組與其權限。
     */
    @Test
    void accountResolvesGroupsAndPermissions() {
        Permission permission = new Permission();
        permission.setCode("USER_CREATE");
        permission.setName("建立帳號");
        permissionRepository.save(permission);

        Group group = new Group();
        group.setCode("ADMIN");
        group.setName("管理員");
        group.setPermissions(Set.of(permission));
        groupRepository.save(group);

        Account account = new Account();
        account.setUsername("bob");
        account.setGroups(Set.of(group));
        accountRepository.save(account);

        Account found = accountRepository.findByUsername("bob").orElseThrow();

        assertThat(found.getGroups()).hasSize(1);
        Group foundGroup = found.getGroups().iterator().next();
        assertThat(foundGroup.getCode()).isEqualTo("ADMIN");
        assertThat(foundGroup.getPermissions())
                .extracting(Permission::getCode)
                .containsExactly("USER_CREATE");
    }

    /**
     * 驗證群組代碼唯一性查詢：{@code findByCode} 可依代碼取回群組。
     */
    @Test
    void groupFindByCodeWorks() {
        Group group = new Group();
        group.setCode("STAFF");
        group.setName("一般員工");
        groupRepository.save(group);

        assertThat(groupRepository.findByCode("STAFF")).isPresent();
        assertThat(groupRepository.findByCode("UNKNOWN")).isEmpty();
    }

    /**
     * 驗證權限代碼唯一性查詢：{@code findByCode} 可依代碼取回權限。
     */
    @Test
    void permissionFindByCodeWorks() {
        Permission permission = new Permission();
        permission.setCode("USER_DELETE");
        permission.setName("刪除帳號");
        permissionRepository.save(permission);

        assertThat(permissionRepository.findByCode("USER_DELETE")).isPresent();
        assertThat(permissionRepository.findByCode("UNKNOWN")).isEmpty();
    }
}
