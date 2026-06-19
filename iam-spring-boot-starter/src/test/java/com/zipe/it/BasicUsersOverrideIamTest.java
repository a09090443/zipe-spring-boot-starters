package com.zipe.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.security.IamUserDetailsService;
import com.zipe.service.BasicUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 驗證 {@code starters_example} 採用的覆寫策略在「帶 iam」情境下成立：
 *
 * <p>於應用層宣告 logon 的 {@link BasicUserServiceImpl} Bean（如範例的
 * {@code BasicUserServiceConfig}），可使 iam 的 {@link IamUserDetailsService} 因
 * {@code @ConditionalOnMissingBean} 退讓，讓 {@code security.basic.users} 成為 BASIC 模式的帳號來源。</p>
 */
@SpringBootTest(
        classes = IamItApplication.class,
        properties = {
                "security.basic.users[0].username=user01",
                "security.basic.users[0].password=1234",
                "security.basic.users[0].authorities[0]=ORDER_EXPORT"
        })
@Import(BasicUsersOverrideIamTest.OverrideConfig.class)
class BasicUsersOverrideIamTest {

    /** 容器中的使用者服務，預期為應用層覆寫的純 {@link BasicUserServiceImpl}（非 iam 子類）。 */
    @Autowired
    private BasicUserServiceImpl basicUserService;

    /**
     * 模擬範例 {@code BasicUserServiceConfig}：應用層宣告 logon 的 {@link BasicUserServiceImpl}。
     */
    @TestConfiguration
    static class OverrideConfig {
        @Bean
        BasicUserServiceImpl basicUserServiceImpl(PasswordEncoder passwordEncoder,
                                                  SecurityPropertyConfig securityPropertyConfig) {
            return new BasicUserServiceImpl(passwordEncoder, securityPropertyConfig);
        }
    }

    /**
     * 應用層的 {@link BasicUserServiceImpl} Bean 應使 iam 的 {@link IamUserDetailsService} 退讓。
     */
    @Test
    void applicationBeanOverridesIamUserDetailsService() {
        assertThat(basicUserService).isNotInstanceOf(IamUserDetailsService.class);
    }

    /**
     * 覆寫生效後，{@code security.basic.users} 成為帳號來源，且 authorities 來自設定檔。
     */
    @Test
    void basicUsersTakeEffect() {
        UserDetails details = basicUserService.loadUserByUsername("user01");

        assertThat(details.getUsername()).isEqualTo("user01");
        assertThat(details.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ORDER_EXPORT");
    }
}
