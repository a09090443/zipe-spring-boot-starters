package com.example.jdbc;

import com.zipe.jdbc.BaseJDBC;
import org.springframework.stereotype.Repository;

/**
 * JDBC 查詢範例類別，示範如何繼承 {@link com.zipe.jdbc.BaseJDBC} 以取得基礎的 JDBC 操作能力。
 * <p>
 * 本類別作為 {@code starters_example} 專案中 db-spring-boot-starter 的使用範例，
 * 透過繼承 BaseJDBC 即可直接使用其提供的通用查詢、更新等方法，無需額外配置。
 * </p>
 *
 * @author Gary.Tsai
 */
@Repository
public class ExampleJdbc extends BaseJDBC {}
