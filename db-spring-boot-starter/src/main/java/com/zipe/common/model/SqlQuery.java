package com.zipe.common.model;

import com.zipe.jdbc.criteria.Conditions;
import lombok.Data;

import java.util.Map;

/**
 * SQL 查詢封裝模型，用於傳遞執行 SQL 所需的全部參數。
 *
 * <p>封裝了 SQL 檔案路徑、目標回傳型別、查詢條件及額外參數，
 * 供 {@code BaseJDBC} 等 JDBC 工具類別統一接收並執行對應的 SQL 檔案。</p>
 *
 * <p>提供兩種建構方式：</p>
 * <ul>
 *   <li>僅指定 SQL 檔案名稱（使用預設目錄）</li>
 *   <li>同時指定目錄與檔案名稱（適用於非預設路徑的 SQL 檔案）</li>
 * </ul>
 *
 * @param <T> 查詢結果映射的目標型別
 * @author : Gary Tsai
 * @created : @Date 2020/11/10 下午 04:44
 **/
@Data
public class SqlQuery<T> {

    /** SQL 檔案所在目錄路徑；若未指定則使用預設目錄 */
    private String sqlDir;

    /** SQL 檔案名稱（不含路徑），對應 classpath 下的 SQL 資源檔 */
    private String sqlFileName;

    /** 查詢結果要映射的目標類別型別 */
    private Class<T> clazz;

    /** 查詢條件物件，包含 WHERE 子句所需的篩選條件 */
    private Conditions conditions;

    /** 額外的具名參數 Map，鍵為 SQL 中的佔位符名稱，值為對應參數值 */
    private Map<String, Object> params;

    /**
     * 使用預設目錄建立 SQL 查詢物件。
     *
     * <p>適用於 SQL 檔案放置於系統預設目錄的情境，無需額外指定目錄路徑。</p>
     *
     * @param sqlFileName SQL 檔案名稱（不含目錄）
     * @param clazz       查詢結果映射的目標類別
     * @param conditions  查詢條件
     * @param params      額外具名參數
     */
    public SqlQuery(String sqlFileName, Class<T> clazz, Conditions conditions, Map<String, Object> params) {
        this.sqlFileName = sqlFileName;
        this.clazz = clazz;
        this.conditions = conditions;
        this.params = params;
    }

    /**
     * 指定目錄與檔案名稱建立 SQL 查詢物件。
     *
     * <p>適用於 SQL 檔案放置於非預設目錄的情境，需明確傳入目錄路徑。</p>
     *
     * @param sqlDir      SQL 檔案所在目錄路徑
     * @param sqlFileName SQL 檔案名稱（不含目錄）
     * @param clazz       查詢結果映射的目標類別
     * @param conditions  查詢條件
     * @param params      額外具名參數
     */
    public SqlQuery(String sqlDir, String sqlFileName, Class<T> clazz, Conditions conditions, Map<String, Object> params) {
        this.sqlDir = sqlDir;
        this.sqlFileName = sqlFileName;
        this.clazz = clazz;
        this.conditions = conditions;
        this.params = params;
    }

}
