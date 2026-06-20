package com.example.service;

import com.example.model.UserDetail;
import com.example.model.UserMain;

/**
 * 資料庫範例服務介面。
 *
 * <p>定義以使用者名稱查詢 {@link UserMain} 與 {@link UserDetail} 的基本操作，
 * 示範透過 db-spring-boot-starter 進行多資料來源存取的使用方式。
 */
public interface DBExampleService {

  /**
   * 依使用者名稱查詢使用者主要資料。
   *
   * @param name 使用者名稱
   * @return 對應的 {@link UserMain} 物件；若查無資料則回傳 {@code null}
   */
  UserMain getUserMainByName(String name);

  /**
   * 切換至指定資料來源後，依使用者名稱查詢使用者主要資料。
   *
   * <p>示範以程式碼動態指定資料來源：查詢前切換至 {@code dataSourceName}，
   * 查詢結束後還原（清除）{@code DataSourceHolder}，避免污染同執行緒的後續請求。</p>
   *
   * @param name           使用者名稱
   * @param dataSourceName 目標資料來源名稱（須為 data-source.properties 中實際存在的資料源）
   * @return 對應的 {@link UserMain} 物件；若該資料源查無資料則回傳 {@code null}
   */
  UserMain getUserMainByName(String name, String dataSourceName);

  /**
   * 依使用者名稱查詢使用者詳細資料。
   *
   * @param name 使用者名稱
   * @return 對應的 {@link UserDetail} 物件；若查無資料則回傳 {@code null}
   */
  UserDetail getUserDetailByName(String name);
}
