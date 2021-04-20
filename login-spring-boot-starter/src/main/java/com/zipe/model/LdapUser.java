package com.zipe.model;

import lombok.Data;

/**
 * @author gary.tsai 2019/6/28
 */
@Data
public class LdapUser {

    private String userId;

    private String name;

    private String email;

    private String ldapDn;

    private String isEnabled;
}
