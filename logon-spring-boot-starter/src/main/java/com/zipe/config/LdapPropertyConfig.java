package com.zipe.config;

import lombok.Data;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/20 下午 21:02
 **/
@Data
public class LdapPropertyConfig {
    private String ip = "127.0.0.1";
    private String domain = "ldap.zipe.com";
    private String port = "389";
    private String dn = "DC=zipe,DC=local";
}
