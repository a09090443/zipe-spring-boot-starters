package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 下午 21:02
 **/
@Configuration
@ConfigurationProperties(prefix = "ldap")
@Data
public class LdapPropertyConfig {
    private String ip;
    private String domain;
    private String port;
    private String dn;
}
