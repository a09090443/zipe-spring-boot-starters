package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/19 下午 21:15
 **/
@Configuration
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityPropertyConfig {

    private Boolean enable = Boolean.TRUE;
    private String verificationType;
    private Boolean recordLogEnable = Boolean.FALSE;
    private String customRecordLogBean;
    private String allowUris;
    private String loginUri;
    private String loginSuccessUri = "/";
    private String loginFailureUri = "/error";
    private String customBeanName;
    private LdapPropertyConfig ldap = new LdapPropertyConfig();
}
