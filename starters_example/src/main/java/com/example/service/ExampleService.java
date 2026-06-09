package com.example.service;

/**
 * 通用範例服務介面，示範 starters_example 模組的基本服務層用法。
 * <p>
 * 實作類別由 Spring 容器管理，供 Controller 層注入使用。
 * </p>
 */
public interface ExampleService {

  /**
   * 依傳入的姓名產生問候語字串。
   *
   * @param name 欲問候的對象名稱
   * @return 格式化後的問候語字串
   */
  String sayHello(String name);

}
