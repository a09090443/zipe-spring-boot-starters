package com.zipe.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.zipe.autoconfiguration.IamAutoConfiguration;
import com.zipe.entity.Account;
import com.zipe.repository.AccountRepository;
import com.zipe.security.DbGrantedAuthoritiesResolver;
import com.zipe.security.GrantedAuthoritiesResolver;
import com.zipe.security.IamUserDetailsService;
import com.zipe.service.BasicUserServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 驗證 {@link GrantedAuthoritiesResolver} 的覆寫機制（{@code @ConditionalOnMissingBean}）。
 * <p>
 * 以 {@link ApplicationContextRunner} 載入 {@link IamAutoConfiguration}，比對兩種情境：
 * </p>
 * <ul>
 *   <li>未提供自訂 Bean 時，容器中的解析器為 iam 預設的 {@link DbGrantedAuthoritiesResolver}；</li>
 *   <li>提供同型別的自訂 Bean 時，iam 預設自動退讓，容器採用自訂實作。</li>
 * </ul>
 * 採輕量 context runner 以精確驗證條件式裝配的退讓行為（auto-configuration 階段即生效）。
 *
 * @author Gary.Tsai
 */
class GrantedAuthoritiesResolverOverrideTest {

    /** 共用的 context runner：載入 iam 自動配置與支撐 JPA 所需的基礎自動配置與 H2 設定。 */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    IamAutoConfiguration.class))
            .withUserConfiguration(SupportConfig.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:iamoverride;DB_CLOSE_DELAY=-1;MODE=LEGACY",
                    "spring.jpa.hibernate.ddl-auto=create-drop");

    /**
     * 未提供自訂 Bean 時，容器解析器應為 iam 預設的 {@link DbGrantedAuthoritiesResolver}。
     */
    @Test
    void usesIamDefaultWhenNoCustomBean() {
        contextRunner.run(context -> {
            assertThat(context)
                    .getBean(GrantedAuthoritiesResolver.class)
                    .isInstanceOf(DbGrantedAuthoritiesResolver.class);
            assertThat(context.getBeansOfType(GrantedAuthoritiesResolver.class)).hasSize(1);
        });
    }

    /**
     * 提供同型別自訂 Bean 時，iam 預設應退讓，容器採用自訂實作，且解析結果來自自訂邏輯。
     */
    @Test
    void customResolverOverridesIamDefault() {
        contextRunner.withUserConfiguration(OverrideConfig.class).run(context -> {
            GrantedAuthoritiesResolver resolver = context.getBean(GrantedAuthoritiesResolver.class);
            assertThat(resolver).isNotInstanceOf(DbGrantedAuthoritiesResolver.class);
            assertThat(resolver.resolve("anyone").stream().map(GrantedAuthority::getAuthority))
                    .containsExactly("CUSTOM_AUTHORITY");
        });
    }

    /**
     * 覆寫後容器中僅剩自訂 Bean（預設 Bean 未同時存在），且經 iam 使用者服務載入帳號時，
     * 授權結果實際採用自訂 Bean 回傳的權限。
     */
    @Test
    void customResolverIsTheOnlyBeanAndDrivesUserAuthorities() {
        contextRunner.withUserConfiguration(OverrideConfig.class).run(context -> {
            assertThat(context.getBeansOfType(GrantedAuthoritiesResolver.class))
                    .containsOnlyKeys("customResolver");

            Account account = new Account();
            account.setUsername("override-user");
            account.setPassword("{noop}secret");
            account.setEnabled(Boolean.TRUE);
            account.setLocked(Boolean.FALSE);
            context.getBean(AccountRepository.class).save(account);

            BasicUserServiceImpl userService = context.getBean(BasicUserServiceImpl.class);
            assertThat(userService).isInstanceOf(IamUserDetailsService.class);
            UserDetails user = userService.loadUserByUsername("override-user");
            assertThat(user.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                    .containsExactly("CUSTOM_AUTHORITY");
        });
    }

    /**
     * 補足 iam 自動配置所需、平時由 logon 容器提供的 {@link PasswordEncoder}，
     * 使最小情境得以啟動。實體掃描與 Repository 掃描由 {@link IamAutoConfiguration} 自身
     * 的 {@code @EntityScan} / {@code @EnableJpaRepositories} 提供。
     */
    @Configuration(proxyBeanMethods = false)
    static class SupportConfig {

        /**
         * 提供密碼編碼器，對應實際環境由 logon 的 {@code SecurityConfiguration} 提供。
         *
         * @return BCrypt 密碼編碼器
         */
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    /**
     * 提供覆寫用的自訂 {@link GrantedAuthoritiesResolver} Bean。
     */
    @Configuration(proxyBeanMethods = false)
    static class OverrideConfig {

        /**
         * 自訂解析器：對任何帳號皆回傳固定的 {@code CUSTOM_AUTHORITY}。
         *
         * @return 自訂的授權解析器
         */
        @Bean
        GrantedAuthoritiesResolver customResolver() {
            return username -> List.<GrantedAuthority>of(new SimpleGrantedAuthority("CUSTOM_AUTHORITY"));
        }
    }
}
