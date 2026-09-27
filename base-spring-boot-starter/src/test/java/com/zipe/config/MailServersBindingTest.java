package com.zipe.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 驗證 {@code mail.servers} 多組 SMTP 設定的屬性繫結（情境計畫 SC-01）。
 *
 * <p>對應需求 REQ-MAIL-FAILOVER-001：多組 SMTP 伺服器須以有序清單表達，
 * 各組可獨立設定 host / port / username / 密碼 / smtp-auth-enable / smtp-start-tls-enable /
 * transport-protocol / encrypt-enable，且清單順序即為容錯切換的優先序。</p>
 */
class MailServersBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableMailPropertiesConfig.class)
            .withPropertyValues(
                    "mail.servers[0].name=primary",
                    "mail.servers[0].host=127.0.0.1",
                    "mail.servers[0].port=3025",
                    "mail.servers[0].username=userA",
                    "mail.servers[0].pa55word=pwA",
                    "mail.servers[0].smtp-auth-enable=true",
                    "mail.servers[0].smtp-start-tls-enable=false",
                    "mail.servers[0].transport-protocol=smtp",
                    "mail.servers[1].name=backup",
                    "mail.servers[1].host=127.0.0.1",
                    "mail.servers[1].port=3026",
                    "mail.servers[1].username=userB",
                    "mail.servers[1].pa55word=cHdCMTIz",
                    "mail.servers[1].smtp-auth-enable=true",
                    "mail.servers[1].smtp-start-tls-enable=true",
                    "mail.servers[1].transport-protocol=smtps",
                    "mail.servers[1].encrypt-enable=true");

    /** SC-01：多組 SMTP 設定可正確繫結載入，清單順序即為優先序。 */
    @Test
    void bindsMultipleServersInDeclaredOrder() {
        contextRunner.run(context -> {
            MailPropertyConfig config = context.getBean(MailPropertyConfig.class);
            assertThat(config.getServers()).hasSize(2);

            MailServerProperty primary = config.getServers().get(0);
            assertThat(primary.getName()).isEqualTo("primary");
            assertThat(primary.getHost()).isEqualTo("127.0.0.1");
            assertThat(primary.getPort()).isEqualTo("3025");
            assertThat(primary.getUsername()).isEqualTo("userA");
            assertThat(primary.getPa55word()).isEqualTo("pwA");
            assertThat(primary.getSmtpAuthEnable()).isTrue();
            assertThat(primary.getSmtpStartTlsEnable()).isFalse();
            assertThat(primary.getTransportProtocol()).isEqualTo("smtp");
            assertThat(primary.getEncryptEnable()).isFalse();

            MailServerProperty backup = config.getServers().get(1);
            assertThat(backup.getName()).isEqualTo("backup");
            assertThat(backup.getHost()).isEqualTo("127.0.0.1");
            assertThat(backup.getPort()).isEqualTo("3026");
            assertThat(backup.getUsername()).isEqualTo("userB");
            assertThat(backup.getPa55word()).isEqualTo("cHdCMTIz");
            assertThat(backup.getSmtpAuthEnable()).isTrue();
            assertThat(backup.getSmtpStartTlsEnable()).isTrue();
            assertThat(backup.getTransportProtocol()).isEqualTo("smtps");
            assertThat(backup.getEncryptEnable()).isTrue();

            // 清單順序即優先序：索引 0 為 primary、索引 1 為 backup
            assertThat(config.getServers()).extracting(MailServerProperty::getName)
                    .containsExactly("primary", "backup");
        });
    }

    private final ApplicationContextRunner threeServerContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableMailPropertiesConfig.class)
            .withPropertyValues(
                    "mail.servers[0].name=primary",
                    "mail.servers[0].host=127.0.0.1",
                    "mail.servers[0].port=3025",
                    "mail.servers[0].username=userA",
                    "mail.servers[0].pa55word=pwA",
                    "mail.servers[0].smtp-auth-enable=true",
                    "mail.servers[0].smtp-start-tls-enable=false",
                    "mail.servers[0].transport-protocol=smtp",
                    "mail.servers[1].name=secondary",
                    "mail.servers[1].host=127.0.0.2",
                    "mail.servers[1].port=3026",
                    "mail.servers[1].username=userB",
                    "mail.servers[1].pa55word=cHdCMTIz",
                    "mail.servers[1].smtp-auth-enable=true",
                    "mail.servers[1].smtp-start-tls-enable=true",
                    "mail.servers[1].transport-protocol=smtps",
                    "mail.servers[1].encrypt-enable=true",
                    "mail.servers[2].host=127.0.0.3",
                    "mail.servers[2].port=3027");

    /**
     * SC-001：三組 mail.servers 依索引順序繫結，各組欄位彼此獨立（互不污染），
     * 且第三組未設定的欄位（name、username、pa55word、encrypt-enable、smtp-auth-enable、
     * smtp-start-tls-enable、transport-protocol）皆採用各自預設值而非承接第一、二組的值。
     */
    @Test
    void bindsThreeServersInDeclaredOrder_withIndependentFieldsAndDefaults() {
        threeServerContextRunner.run(context -> {
            MailPropertyConfig config = context.getBean(MailPropertyConfig.class);
            assertThat(config.getServers()).hasSize(3);

            assertThat(config.getServers()).extracting(MailServerProperty::getName)
                    .containsExactly("primary", "secondary", null);
            assertThat(config.getServers()).extracting(MailServerProperty::getHost)
                    .containsExactly("127.0.0.1", "127.0.0.2", "127.0.0.3");
            assertThat(config.getServers()).extracting(MailServerProperty::getPort)
                    .containsExactly("3025", "3026", "3027");

            MailServerProperty third = config.getServers().get(2);
            // 第三組未設定任何帳密／協定欄位，須為各自的預設值，不得承接第一、二組的設定
            assertThat(third.getUsername()).isNull();
            assertThat(third.getPa55word()).isNull();
            assertThat(third.getEncryptEnable()).isFalse();
            assertThat(third.getSmtpAuthEnable()).isTrue();
            assertThat(third.getSmtpStartTlsEnable()).isFalse();
            assertThat(third.getTransportProtocol()).isEqualTo("smtp");

            // 三組彼此隔離：即使第二組開啟 encrypt-enable／smtps，也不影響第一、三組
            assertThat(config.getServers().get(0).getEncryptEnable()).isFalse();
            assertThat(config.getServers().get(0).getTransportProtocol()).isEqualTo("smtp");
        });
    }

    /** 未設定 servers 時，resolveServers() 應以既有扁平欄位合成單一組（向後相容，見 REQ-008）。 */
    @Test
    void resolveServersFallsBackToFlatFieldsWhenServersEmpty() {
        MailPropertyConfig config = new MailPropertyConfig();
        config.setHost("legacy.example.com");
        config.setPort("25");
        config.setUsername("legacyUser");
        config.setPa55word("legacyPw");

        assertThat(config.getServers()).isEmpty();
        assertThat(config.resolveServers()).hasSize(1);
        assertThat(config.resolveServers().get(0).getHost()).isEqualTo("legacy.example.com");
        assertThat(config.resolveServers().get(0).getUsername()).isEqualTo("legacyUser");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MailPropertyConfig.class)
    static class EnableMailPropertiesConfig {}
}
