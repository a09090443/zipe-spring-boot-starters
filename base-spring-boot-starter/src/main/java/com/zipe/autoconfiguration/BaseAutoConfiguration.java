package com.zipe.autoconfiguration;

import com.zipe.config.MailPropertyConfig;
import com.zipe.config.VelocityPropertyConfig;
import com.zipe.service.MailService;
import com.zipe.service.impl.MailServiceImpl;
import com.zipe.util.ApplicationContextHelper;
import com.zipe.util.VelocityUtil;
import com.zipe.util.string.StringConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/15 下午 03:15
 **/
@Configuration
@PropertySource({"classpath:resource.properties"})
@ConditionalOnClass({VelocityPropertyConfig.class, MailPropertyConfig.class})
@EnableConfigurationProperties({VelocityPropertyConfig.class, MailPropertyConfig.class})
public class BaseAutoConfiguration {

    private final VelocityPropertyConfig velocityPropertyConfig;

    private final MailPropertyConfig mailPropertyConfig;

    @Autowired
    BaseAutoConfiguration(VelocityPropertyConfig velocityPropertyConfig, MailPropertyConfig mailPropertyConfig) {
        this.velocityPropertyConfig = velocityPropertyConfig;
        this.mailPropertyConfig = mailPropertyConfig;
    }

    @Bean
    @ConditionalOnResource(resources = "classpath:message.properties")
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:message");
        messageSource.setDefaultEncoding(StringConstant.ENCODE_UTF8);
        return messageSource;
    }

    @Bean
    public ApplicationContextHelper applicationContextHelper() {
        return new ApplicationContextHelper();
    }

    @Bean
    public VelocityUtil velocityUtil() {
        VelocityUtil velocityUtil = new VelocityUtil();
        velocityUtil.setDir(velocityPropertyConfig.getDirPath());
        velocityUtil.initClassPath();
        return velocityUtil;
    }

    @Bean
    public MailService mailService() {
        return new MailServiceImpl(mailPropertyConfig);
    }

}
