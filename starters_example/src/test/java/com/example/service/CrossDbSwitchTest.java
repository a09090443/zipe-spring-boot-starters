package com.example.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.base.TestBase;
import com.example.repository.UserMainRepository;
import com.zipe.base.database.DataSourceHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 驗證 db-spring-boot-starter 可在「不同資料庫類型」之間動態切換（MySQL ↔ PostgreSQL）。
 *
 * <p>example1 為 MySQL（僅有 {@code OnlyExample1}）、postgres 為 PostgreSQL（僅有 {@code OnlyPostgres}），
 * 兩者互無對方的獨有資料。透過 {@link DataSourceHolder} 切換後查詢，
 * 若切到 MySQL 只查得到 OnlyExample1、切到 PostgreSQL 只查得到 OnlyPostgres，
 * 即可證明查詢確實跨資料庫類型路由，而非 fallback 回 primary。</p>
 */
class CrossDbSwitchTest extends TestBase {

  private final UserMainRepository userMainRepository;

  @Autowired
  CrossDbSwitchTest(UserMainRepository userMainRepository) {
    this.userMainRepository = userMainRepository;
  }

  @AfterEach
  void clearHolder() {
    DataSourceHolder.clearDataSourceName();
  }

  @Test
  void switchBetweenMysqlAndPostgres() {
    // 切換至 example1（MySQL）：只查得到 MySQL 的獨有資料
    DataSourceHolder.setDataSourceName("example1");
    assertNotNull(userMainRepository.findUserByName("OnlyExample1"),
        "切到 MySQL(example1) 後應查得到 OnlyExample1");
    assertNull(userMainRepository.findUserByName("OnlyPostgres"),
        "切到 MySQL(example1) 後不應查得到 PostgreSQL 獨有的 OnlyPostgres");

    // 切換至 postgres（PostgreSQL）：只查得到 PostgreSQL 的獨有資料
    DataSourceHolder.setDataSourceName("postgres");
    assertNotNull(userMainRepository.findUserByName("OnlyPostgres"),
        "切到 PostgreSQL(postgres) 後應查得到 OnlyPostgres");
    assertNull(userMainRepository.findUserByName("OnlyExample1"),
        "切到 PostgreSQL(postgres) 後不應查得到 MySQL 獨有的 OnlyExample1");
  }
}
