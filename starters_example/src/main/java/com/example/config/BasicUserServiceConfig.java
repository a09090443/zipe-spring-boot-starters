package com.example.config;

import com.zipe.config.SecurityPropertyConfig;
import com.zipe.service.BasicUserServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 讓 BASIC 模式的 {@code security.basic.users} 在「帶 iam」的範例中生效。
 *
 * <p>背景：iam-starter 的 {@code IamUserDetailsService} 以 {@link BasicUserServiceImpl} 型別、
 * {@code @ConditionalOnMissingBean} 註冊，預設會接管 BASIC 模式的帳號查詢、改查 {@code iam_account}，
 * 使 {@code security.basic.users} 不生效。</p>
 *
 * <p>本設定在「應用層」宣告一個 {@link BasicUserServiceImpl} Bean——應用層 Bean 先於
 * autoconfiguration 註冊，故 iam 的 {@code iamUserDetailsService} 因 {@code @ConditionalOnMissingBean}
 * 自動退讓。如此 BASIC 模式的帳號與權限改由 {@code application.yml} 的 {@code security.basic.users} 提供
 * （登入帳密與 authorities 皆來自設定檔，不再經 iam 帳號表與群組／權限解析）。</p>
 *
 * <p>注意：這是「為了體驗 basic.users」而刻意覆寫 iam 的帳號接管。若要回到 iam 帳號表驗證，
 * 移除本設定即可恢復 {@code IamUserDetailsService} 的接管。</p>
 *
 * @author Gary Tsai
 */
@Configuration
public class BasicUserServiceConfig {

    /**
     * 宣告 logon 內建的 {@link BasicUserServiceImpl}，覆寫 iam 的 {@code IamUserDetailsService}，
     * 使 {@code security.basic.users} 成為 BASIC 模式的帳號來源。
     *
     * @param passwordEncoder        密碼編碼器（logon 容器提供）
     * @param securityPropertyConfig Security 設定屬性（持有 {@code security.basic.users}）
     * @return 讀取 {@code security.basic.users} 的 {@link BasicUserServiceImpl}
     */
    @Bean
    public BasicUserServiceImpl basicUserServiceImpl(PasswordEncoder passwordEncoder,
                                                     SecurityPropertyConfig securityPropertyConfig) {
        return new BasicUserServiceImpl(passwordEncoder, securityPropertyConfig);
    }
}
