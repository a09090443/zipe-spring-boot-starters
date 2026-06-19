package com.zipe.autoconfiguration;

import com.zipe.config.IamProperties;
import com.zipe.controller.AccountController;
import com.zipe.controller.GroupController;
import com.zipe.controller.PermissionController;
import com.zipe.repository.AccountRepository;
import com.zipe.repository.GroupRepository;
import com.zipe.repository.PermissionRepository;
import com.zipe.security.DbGrantedAuthoritiesResolver;
import com.zipe.security.GrantedAuthoritiesResolver;
import com.zipe.security.IamUserDetailsService;
import com.zipe.service.AccountService;
import com.zipe.service.AccountServiceImpl;
import com.zipe.service.BasicUserServiceImpl;
import com.zipe.service.GroupService;
import com.zipe.service.GroupServiceImpl;
import com.zipe.service.PermissionService;
import com.zipe.service.PermissionServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * iam-starter 的自動配置類別，負責裝配帳號／群組／權限的核心 Service、資料庫授權解析器，
 * 以及與 logon-starter 整合的使用者服務。
 * <p>
 * 所有對外 Bean 均標註 {@code @ConditionalOnMissingBean}，作為客製化的總開關：
 * 業務專案於容器中宣告同型別 Bean 即可覆寫預設實作。其中：
 * </p>
 * <ul>
 *   <li>{@link DbGrantedAuthoritiesResolver} 以 {@link GrantedAuthoritiesResolver} 型別宣告，
 *       覆寫 logon 提供的預設空實作，使三種驗證模式皆能取得 DB 權限。</li>
 *   <li>{@link IamUserDetailsService} 以 {@link BasicUserServiceImpl} 型別宣告，使 logon 的
 *       {@code basicUserServiceImpl}（{@code @ConditionalOnMissingBean}）自動退讓，
 *       BASIC／JWT 模式改用資料庫帳號表。</li>
 * </ul>
 * <p>
 * {@link PasswordEncoder} 由 logon 容器提供，於各 Bean 方法以參數注入；本配置以
 * {@code @EntityScan} 與 {@code @EnableJpaRepositories} 指向 starter 套件，引入即生效。
 * </p>
 *
 * @author Gary.Tsai
 */
@AutoConfiguration
@EnableConfigurationProperties(IamProperties.class)
@ConditionalOnProperty(prefix = "iam", name = "enabled", havingValue = "true", matchIfMissing = true)
@EntityScan("com.zipe.entity")
@EnableJpaRepositories("com.zipe.repository")
public class IamAutoConfiguration {

    /**
     * 帳號管理服務，注入帳號／群組 Repository 與密碼編碼器。
     *
     * @param accountRepository 帳號 Repository
     * @param groupRepository   群組 Repository
     * @param passwordEncoder   密碼編碼器（由 logon 容器提供）
     * @return 帳號服務 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AccountService accountService(AccountRepository accountRepository,
                                         GroupRepository groupRepository,
                                         PasswordEncoder passwordEncoder) {
        return new AccountServiceImpl(accountRepository, groupRepository, passwordEncoder);
    }

    /**
     * 群組管理服務，注入群組／權限 Repository。
     *
     * @param groupRepository      群組 Repository
     * @param permissionRepository 權限 Repository
     * @return 群組服務 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public GroupService groupService(GroupRepository groupRepository,
                                     PermissionRepository permissionRepository) {
        return new GroupServiceImpl(groupRepository, permissionRepository);
    }

    /**
     * 權限管理服務，注入權限 Repository。
     *
     * @param permissionRepository 權限 Repository
     * @return 權限服務 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionService permissionService(PermissionRepository permissionRepository) {
        return new PermissionServiceImpl(permissionRepository);
    }

    /**
     * 資料庫授權解析器，以 {@link GrantedAuthoritiesResolver} 型別宣告，
     * 覆寫 logon-starter 的預設空實作。
     *
     * @param accountRepository 帳號 Repository
     * @param iamProperties     iam 設定屬性
     * @return 授權解析器 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public GrantedAuthoritiesResolver grantedAuthoritiesResolver(AccountRepository accountRepository,
                                                                 IamProperties iamProperties) {
        return new DbGrantedAuthoritiesResolver(accountRepository, iamProperties);
    }

    /**
     * iam 使用者服務，以 {@link BasicUserServiceImpl} 型別宣告，使 logon 的預設
     * {@code basicUserServiceImpl} 自動退讓，BASIC 模式改用資料庫帳號表。
     *
     * @param passwordEncoder     密碼編碼器（由 logon 容器提供）
     * @param accountRepository   帳號 Repository
     * @param authoritiesResolver 授權解析器
     * @return 使用者服務 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public BasicUserServiceImpl iamUserDetailsService(PasswordEncoder passwordEncoder,
                                                      AccountRepository accountRepository,
                                                      GrantedAuthoritiesResolver authoritiesResolver) {
        return new IamUserDetailsService(passwordEncoder, accountRepository, authoritiesResolver);
    }

    /**
     * 內建 REST Controller 的巢狀配置，以 {@code @ConditionalOnProperty(prefix="iam.api", name="enabled")}
     * 控制是否裝配（{@code matchIfMissing = true}，預設啟用）。
     * <p>
     * 每個 Controller Bean 皆標註 {@code @ConditionalOnMissingBean}：業務專案可宣告同型別 Bean 覆寫
     * 預設行為，或以 {@code iam.api.enabled=false} 整組關閉後自行實作。各 Controller 的路由前綴由
     * {@code iam.api.base-path}（預設 {@code /api/iam}）透過類別層級 {@code @RequestMapping} 的
     * 屬性佔位符解析而來。
     * </p>
     *
     * @author Gary.Tsai
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "iam.api", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class IamControllerConfiguration {

        /**
         * 帳號管理的內建 REST Controller。
         *
         * @param accountService 帳號管理服務
         * @return 帳號 Controller Bean
         */
        @Bean
        @ConditionalOnMissingBean
        public AccountController accountController(AccountService accountService) {
            return new AccountController(accountService);
        }

        /**
         * 群組管理的內建 REST Controller。
         *
         * @param groupService 群組管理服務
         * @return 群組 Controller Bean
         */
        @Bean
        @ConditionalOnMissingBean
        public GroupController groupController(GroupService groupService) {
            return new GroupController(groupService);
        }

        /**
         * 權限管理的內建 REST Controller。
         *
         * @param permissionService 權限管理服務
         * @return 權限 Controller Bean
         */
        @Bean
        @ConditionalOnMissingBean
        public PermissionController permissionController(PermissionService permissionService) {
            return new PermissionController(permissionService);
        }
    }
}
