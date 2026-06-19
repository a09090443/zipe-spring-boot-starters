package com.zipe.testsupport.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * 測試專用的 JPA 實體，刻意置於 {@code com.example} 以外的獨立套件
 * （模擬如 iam-starter 的 {@code com.zipe.entity}），
 * 用以驗證 db-starter 的 EntityManagerFactory 會併入 {@code @EntityScan} 註冊的套件。
 */
@Entity
public class DummyEntity {

    /** 主鍵。 */
    @Id
    private Long id;
}
