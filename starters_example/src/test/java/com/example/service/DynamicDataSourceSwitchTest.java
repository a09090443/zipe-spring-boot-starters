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
 * 驗證 db-spring-boot-starter 的多資料來源動態切換是否真正生效。
 *
 * <p>example1 僅有 {@code OnlyExample1}、example2 僅有 {@code OnlyExample2}，
 * 兩者互無對方的獨有資料。透過 {@link DataSourceHolder} 切換後查詢，
 * 若切到 example1 只查得到 OnlyExample1、查不到 OnlyExample2，反之亦然，
 * 即可證明查詢確實被路由到不同資料來源，而非 fallback 回 primary。</p>
 */
class DynamicDataSourceSwitchTest extends TestBase {

  private final UserMainRepository userMainRepository;

  @Autowired
  DynamicDataSourceSwitchTest(UserMainRepository userMainRepository) {
    this.userMainRepository = userMainRepository;
  }

  @AfterEach
  void clearHolder() {
    DataSourceHolder.clearDataSourceName();
  }

  @Test
  void switchBetweenExample1AndExample2() {
    // 切換至 example1：只查得到 example1 的獨有資料
    DataSourceHolder.setDataSourceName("example1");
    assertNotNull(userMainRepository.findUserByName("OnlyExample1"),
        "切到 example1 後應查得到 example1 獨有的 OnlyExample1");
    assertNull(userMainRepository.findUserByName("OnlyExample2"),
        "切到 example1 後不應查得到 example2 獨有的 OnlyExample2（若查得到代表未真正切換）");

    // 切換至 example2：只查得到 example2 的獨有資料
    DataSourceHolder.setDataSourceName("example2");
    assertNotNull(userMainRepository.findUserByName("OnlyExample2"),
        "切到 example2 後應查得到 example2 獨有的 OnlyExample2");
    assertNull(userMainRepository.findUserByName("OnlyExample1"),
        "切到 example2 後不應查得到 example1 獨有的 OnlyExample1（若查得到代表未真正切換）");
  }
}
