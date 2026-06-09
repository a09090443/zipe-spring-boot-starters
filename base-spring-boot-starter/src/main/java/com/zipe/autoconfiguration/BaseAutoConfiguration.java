package com.zipe.autoconfiguration;

import com.zipe.config.MailPropertyConfig;
import com.zipe.config.ThreadPoolTaskExecutorConfig;
import com.zipe.config.VelocityPropertyConfig;
import com.zipe.service.MailService;
import com.zipe.service.impl.MailServiceImpl;
import com.zipe.util.ApplicationContextHelper;
import com.zipe.util.VelocityUtil;
import com.zipe.util.string.StringConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * base-spring-boot-starter 的自動配置入口類別。
 * <p>
 * 當 classpath 中存在 {@link VelocityPropertyConfig} 與 {@link MailPropertyConfig} 時，
 * 此類別會自動向 Spring 容器註冊以下 Bean：
 * <ul>
 *   <li>{@link MessageSource}：可重新載入的訊息資源（僅於 message.properties 存在時啟用）</li>
 *   <li>{@link ApplicationContextHelper}：提供靜態方式取得 Spring ApplicationContext</li>
 *   <li>{@link VelocityUtil}：Velocity 樣板引擎工具</li>
 *   <li>{@link MailService}：郵件發送服務</li>
 *   <li>{@code threadPoolTaskExecutor}：共用執行緒池</li>
 * </ul>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/15 下午 03:15
 **/
@AutoConfiguration
@PropertySource({"classpath:resource.properties"})
@ConditionalOnClass({VelocityPropertyConfig.class, MailPropertyConfig.class})
@EnableConfigurationProperties({VelocityPropertyConfig.class, MailPropertyConfig.class})
public class BaseAutoConfiguration {

    /** Velocity 樣板引擎的設定屬性（目錄路徑等）。 */
    private final VelocityPropertyConfig velocityPropertyConfig;

    /** 郵件發送的設定屬性（SMTP 主機、帳號、密碼等）。 */
    private final MailPropertyConfig mailPropertyConfig;

    /**
     * 透過建構子注入所需的設定屬性物件。
     *
     * @param velocityPropertyConfig Velocity 樣板引擎相關設定
     * @param mailPropertyConfig     郵件發送相關設定
     */
    @Autowired
    BaseAutoConfiguration(VelocityPropertyConfig velocityPropertyConfig, MailPropertyConfig mailPropertyConfig) {
        this.velocityPropertyConfig = velocityPropertyConfig;
        this.mailPropertyConfig = mailPropertyConfig;
    }

    /**
     * 建立可重新載入的訊息資源 Bean，對應 classpath 下的 {@code message.properties}。
     * <p>
     * 僅當 {@code classpath:message.properties} 存在時才會註冊此 Bean，
     * 避免在未提供訊息資源的應用中造成啟動錯誤。
     *
     * @return 以 UTF-8 編碼讀取 message.properties 的 {@link MessageSource} 實作
     */
    @Bean
    @ConditionalOnResource(resources = "classpath:message.properties")
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:message");
        messageSource.setDefaultEncoding(StringConstant.ENCODE_UTF8);
        return messageSource;
    }

    /**
     * 建立 {@link ApplicationContextHelper} Bean，供需要以靜態方式取得 Spring Bean 的工具類別使用。
     *
     * @return {@link ApplicationContextHelper} 實例
     */
    @Bean
    public ApplicationContextHelper applicationContextHelper() {
        return new ApplicationContextHelper();
    }

    /**
     * 建立 {@link VelocityUtil} Bean，並依照設定檔指定的目錄路徑初始化樣板引擎的 classpath。
     *
     * @return 已完成初始化的 {@link VelocityUtil} 實例
     */
    @Bean
    public VelocityUtil velocityUtil() {
        VelocityUtil velocityUtil = new VelocityUtil();
        velocityUtil.setDir(velocityPropertyConfig.getDirPath());
        // 依設定的目錄路徑初始化 Velocity 引擎，使樣板可被正確解析
        velocityUtil.initClassPath();
        return velocityUtil;
    }

    /**
     * 建立 {@link MailService} Bean，使用注入的郵件設定屬性初始化實作類別。
     *
     * @return {@link MailServiceImpl} 實例
     */
    @Bean
    public MailService mailService() {
        return new MailServiceImpl(mailPropertyConfig);
    }

    /**
     * 建立名為 {@code threadPoolTaskExecutor} 的共用執行緒池 Bean，
     * 供非同步任務（例如排程、非同步方法）統一使用。
     * <p>
     * 核心與最大執行緒數由 {@link ThreadPoolTaskExecutorConfig} 的常數統一管理，
     * 便於全局調整而不散落在各處硬編碼。
     *
     * @return 已完成設定的 {@link ThreadPoolTaskExecutor} 實例
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor serviceJobTaskExecutor() {
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();
        // 執行緒池維護執行緒的最少數量（核心執行緒數）
        poolTaskExecutor.setCorePoolSize(ThreadPoolTaskExecutorConfig.CORE_POOL_SIZE);
        // 執行緒池維護執行緒的最大數量
        poolTaskExecutor.setMaxPoolSize(ThreadPoolTaskExecutorConfig.MAX_POOL_SIZE);
        // 執行緒池所使用的等待佇列容量
        poolTaskExecutor.setQueueCapacity(200);
        // 超過核心執行緒數的閒置執行緒在終止前的等待時間（秒）
        poolTaskExecutor.setKeepAliveSeconds(30000);
        // 應用程式關閉時等待佇列中的任務全部執行完畢，避免任務遺失
        poolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        return poolTaskExecutor;
    }
}
