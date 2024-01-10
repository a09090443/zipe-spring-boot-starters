package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 **/
@Configuration
@ConfigurationProperties(prefix = "security.ldap")
@Data
public class LdapPropertyConfig {
    /**
     * Ldap server ip
     */
    private String ip;
    /**
     * Ldap domain
     */
    private String domain;
    /**
     * Ldap port
     */
    private String port;
    /**
     * Ldap dn
     * sample:DC=zipe,DC=local
     */
    private String dn = "DC=zipe,DC=local";
}
