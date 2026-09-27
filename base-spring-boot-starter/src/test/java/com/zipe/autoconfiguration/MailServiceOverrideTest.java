package com.zipe.autoconfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import com.zipe.service.MailService;
import com.zipe.service.impl.MailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 驗證 {@code mailService} Bean 的覆寫機制（{@code @ConditionalOnMissingBean}），
 * 對應情境測試計畫 SC-21、需求 REQ-MAIL-FAILOVER-012。
 *
 * <p>以 {@link ApplicationContextRunner} 載入 {@link BaseAutoConfiguration}，比對兩種情境：
 * 未提供自訂 Bean 時使用 Starter 內建的 {@link MailServiceImpl}；
 * 提供同型別自訂 Bean 時，Starter 預設自動退讓。</p>
 */
class MailServiceOverrideTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseAutoConfiguration.class));

    /** 未提供自訂 Bean 時，容器中的 MailService 應為 Starter 內建的 MailServiceImpl。 */
    @Test
    void usesStarterDefaultWhenNoCustomBean() {
        contextRunner.run(
                context -> assertThat(context).getBean(MailService.class).isInstanceOf(MailServiceImpl.class));
    }

    /** 提供同型別自訂 Bean 時，Starter 預設應退讓，容器採用自訂實作。 */
    @Test
    void customMailServiceOverridesStarterDefault() {
        contextRunner.withUserConfiguration(CustomMailServiceConfig.class).run(context -> {
            MailService mailService = context.getBean(MailService.class);
            assertThat(mailService).isNotInstanceOf(MailServiceImpl.class);
            assertThat(mailService).isInstanceOf(StubMailService.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomMailServiceConfig {

        @Bean
        MailService mailService() {
            return new StubMailService();
        }
    }

    static class StubMailService implements MailService {
        @Override
        public void setInitData() {}

        @Override
        public void sendEmail(com.zipe.model.Mail mail) {}

        @Override
        public void simpleMailSend(com.zipe.model.Mail mail) {}

        @Override
        public void attachedSend(com.zipe.model.Mail mail) {}

        @Override
        public void richContentSend(com.zipe.model.Mail mail) {}

        @Override
        public void sendBatchMailWithFile(com.zipe.model.Mail mail) {}
    }
}
