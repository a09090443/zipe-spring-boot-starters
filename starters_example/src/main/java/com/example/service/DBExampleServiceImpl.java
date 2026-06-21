package com.example.service;

import com.example.model.UserDetail;
import com.example.model.UserMain;
import com.example.repository.UserDetailRepository;
import com.example.repository.UserMainRepository;
import com.zipe.base.annotation.DS;
import com.zipe.base.database.DataSourceHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 資料庫範例服務實作類別。
 *
 * <p>示範 db-spring-boot-starter 提供的動態資料來源切換機制，
 * 包含透過 {@link DataSourceHolder} 手動切換資料來源，
 * 以及透過 {@link DS} 注解自動切換的兩種使用方式。
 */
@Slf4j
@Service
public class DBExampleServiceImpl implements DBExampleService {

  /** 使用者主要資料的 JPA Repository，對應預設資料來源。 */
  private final UserMainRepository userMainRepository;

  /** 使用者詳細資料的 JPA Repository，對應由 {@link DS} 注解指定的資料來源。 */
  private final UserDetailRepository userDetailRepository;

  /**
   * 以建構子注入方式初始化所需的 Repository 依賴。
   *
   * @param userMainRepository   使用者主要資料 Repository
   * @param userDetailRepository 使用者詳細資料 Repository
   */
  @Autowired
  DBExampleServiceImpl(UserMainRepository userMainRepository, UserDetailRepository userDetailRepository){
    this.userMainRepository = userMainRepository;
    this.userDetailRepository = userDetailRepository;
  }

  /**
   * 依使用者名稱查詢主要資料，示範以程式碼手動切換資料來源的方式。
   *
   * <p>方法執行前會先記錄目前的資料來源名稱，
   * 接著明確將資料來源切換為 {@code "example2"}，再執行查詢。
   *
   * @param name 要查詢的使用者名稱
   * @return 對應的 {@link UserMain} 實體，若查無資料則回傳 {@code null}
   */
  @Override
  public UserMain getUserMainByName(String name) {
    String dataSourceName = DataSourceHolder.getDataSourceName();
    log.debug("DataSourceName:{}", dataSourceName);
    // 指定 datasource（須為 data-source.properties 中實際存在的資料源名稱）
    DataSourceHolder.setDataSourceName("example2");
    dataSourceName = DataSourceHolder.getDataSourceName();
    log.debug("DataSourceName:{}", dataSourceName);
    return userMainRepository.findUserByName(name);
  }

  /**
   * 切換至指定資料來源後，依使用者名稱查詢主要資料，示範以參數動態指定資料來源的方式。
   *
   * <p>查詢前將 {@link DataSourceHolder} 切換至 {@code dataSourceName}，
   * 並於 {@code finally} 清除 ThreadLocal，避免污染同執行緒的後續請求。</p>
   *
   * @param name           要查詢的使用者名稱
   * @param dataSourceName 目標資料來源名稱（須為 data-source.properties 中實際存在的資料源）
   * @return 對應的 {@link UserMain} 實體，若查無資料則回傳 {@code null}
   */
  @Override
  public UserMain getUserMainByName(String name, String dataSourceName) {
    try {
      DataSourceHolder.setDataSourceName(dataSourceName);
      log.debug("切換資料來源至:{}", DataSourceHolder.getDataSourceName());
      return userMainRepository.findUserByName(name);
    } finally {
      // 查詢結束後清除 ThreadLocal，避免污染同執行緒的後續請求
      DataSourceHolder.clearDataSourceName();
    }
  }

  /**
   * 依使用者名稱查詢詳細資料，示範以 {@link DS} 注解自動切換資料來源的方式。
   *
   * <p>{@code @DS} 注解會在方法執行前由 AOP 攔截，
   * 自動將資料來源切換至注解所設定的目標，無需手動操作 {@link DataSourceHolder}。
   *
   * @param name 要查詢的使用者名稱
   * @return 對應的 {@link UserDetail} 實體，若查無資料則回傳 {@code null}
   */
  @Override
  @DS("example2")
  public UserDetail getUserDetailByName(String name) {
    return userDetailRepository.findByName(name);
  }
}
