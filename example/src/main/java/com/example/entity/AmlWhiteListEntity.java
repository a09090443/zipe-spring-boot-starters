package com.example.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@Entity
@Table(name = "AML_WHITE_LIST")
public class AmlWhiteListEntity {
    @NotNull
    @Column(name = "ID", nullable = false)
    @Id
    private Long id;

    @Size(max = 16)
    @Column(name = "CUSTOMER_ID", length = 16)
    private String customerId;

    @Size(max = 16)
    @Column(name = "BANNED_USER_ID", length = 16)
    private String bannedUserId;

}