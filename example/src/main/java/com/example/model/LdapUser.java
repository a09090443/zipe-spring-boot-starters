package com.example.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Gary.Tsai
 */
@Data
@Entity
@Table(name = "LDAP_USER")
public class LdapUser {

    @Id
    private String userId;

    private String name;

    private String email;

    private String ldapDn;

    private String isEnabled;
}
