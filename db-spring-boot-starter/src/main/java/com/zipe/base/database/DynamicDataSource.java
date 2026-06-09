package com.zipe.base.database;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 動態資料來源實作，支援在執行期依據目前執行緒的設定自動切換資料來源。
 *
 * <p>繼承 Spring 的 {@link AbstractRoutingDataSource}，透過覆寫
 * {@link #determineCurrentLookupKey()} 方法，從 {@link DataSourceHolder}
 * 取得目前執行緒所指定的資料來源名稱，進而路由至對應的實體資料來源。
 *
 * <p>典型使用方式為搭配 AOP 切面（{@code DynamicDataSourceAspect}）與
 * {@code @DS} 或 {@code @DynamicDS} 注解，在方法執行前後自動設定與清除
 * ThreadLocal 中的資料來源名稱，實現無侵入式的多資料來源動態切換。
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     * 決定目前執行緒應使用的資料來源查找鍵（lookup key）。
     *
     * <p>此方法由 {@link AbstractRoutingDataSource} 在每次取得連線前呼叫，
     * 回傳值須與初始化時注冊的目標資料來源 key 相符，框架才能正確路由。
     *
     * @return 目前執行緒所指定的資料來源名稱；若未設定則回傳 {@code null}，
     *         框架將自動使用預設資料來源
     */
    @Override
    protected Object determineCurrentLookupKey() {
        // 從 ThreadLocal 取得目前執行緒所指定的資料來源名稱，以決定路由目標
        return DataSourceHolder.getDataSourceName();
    }
}
