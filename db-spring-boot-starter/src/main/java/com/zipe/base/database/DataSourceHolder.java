package com.zipe.base.database;

import java.util.ArrayList;
import java.util.List;

/**
 * 資料來源持有者，負責以 ThreadLocal 方式保存當前執行緒所使用的資料來源名稱。
 *
 * <p>在多資料來源切換場景中，AOP 切面或業務程式碼透過此類別設定目標資料來源，
 * {@link DynamicDataSource} 再從中讀取名稱以決定實際路由的資料來源。
 * 每次請求結束後應呼叫 {@link #clearDataSourceName()} 清除 ThreadLocal，
 * 避免執行緒池環境下資料來源洩漏至下一次請求。</p>
 */
public class DataSourceHolder {

    /**
     * 儲存當前執行緒的資料來源名稱。
     * 使用 ThreadLocal 確保多執行緒環境下各執行緒的資料來源名稱互不干擾。
     */
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    /** 系統啟動時載入的所有可用資料來源名稱清單，供 {@link #containsDataSource(String)} 驗證使用。 */
    public static List<String> dataSourceNames = new ArrayList<>();

    /**
     * 設定當前執行緒所使用的資料來源名稱。
     *
     * @param name 資料來源名稱，須對應 {@link #dataSourceNames} 中已註冊的名稱
     */
    public static void setDataSourceName(String name) {
        contextHolder.set(name);
    }

    /**
     * 取得當前執行緒所設定的資料來源名稱。
     *
     * @return 當前執行緒的資料來源名稱；若尚未設定則回傳 {@code null}
     */
    public static String getDataSourceName() {
        return contextHolder.get();
    }

    /**
     * 清除當前執行緒的資料來源名稱設定。
     *
     * <p>應於每次請求或交易結束後呼叫，防止執行緒池環境下 ThreadLocal 殘留
     * 導致後續請求錯誤路由至非預期的資料來源。</p>
     */
    public static void clearDataSourceName() {
        // 使用 remove() 而非 set(null)，可完整釋放 ThreadLocal Entry，避免記憶體洩漏
        contextHolder.remove();
    }

    /**
     * 判斷指定名稱的資料來源是否已於系統中註冊。
     *
     * @param dataSourceName 欲檢查的資料來源名稱
     * @return 若 {@link #dataSourceNames} 包含該名稱則回傳 {@code true}，否則回傳 {@code false}
     */
    public static boolean containsDataSource(String dataSourceName) {
        return dataSourceNames.contains(dataSourceName);
    }
}
