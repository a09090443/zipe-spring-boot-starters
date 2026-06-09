package com.zipe.jdbc;

import com.zipe.enums.ResourceEnum;
import com.zipe.jdbc.criteria.Conditions;
import com.zipe.jdbc.criteria.Paging;
import com.zipe.util.file.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基礎 JDBC 操作抽象類別。
 *
 * <p>封裝以 {@link ResourceEnum} 定位外部 SQL 檔案的載入、快取與執行流程，
 * 提供 update / queryForBean / queryForMap / queryForList 等常用資料庫操作方法，
 * 並整合 {@link Conditions}（動態條件）與 {@link Paging}（分頁）支援。</p>
 *
 * <p>子類別透過繼承此類別取得完整的 JDBC 操作能力，
 * 底層依賴 Spring 的 {@link NamedParameterJdbcDaoSupport} 來執行具名參數 SQL。</p>
 */
@Slf4j
public abstract class BaseJDBC {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected NamedParameterJdbcDaoSupport support;

    /** 以 SQL 資源識別碼為鍵的 SQL 文字快取，避免重複讀取磁碟或 classpath 資源 */
    private static final Map<String, String> SQL_CACHE = new ConcurrentHashMap<>();

    /**
     * 執行更新操作，不帶參數。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @return 受影響的資料列數
     */
    public int update(ResourceEnum resource) {
        return update(resource, new HashMap<>());
    }

    /**
     * 執行帶具名參數的更新操作。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   具名參數 Map，鍵對應 SQL 中的 {@code :paramName} 佔位符
     * @return 受影響的資料列數
     */
    public int update(ResourceEnum resource, Map<String, Object> params) {
        String sql = getSqlText(resource);
        return support.getNamedParameterJdbcTemplate().update(sql, params);
    }

    /**
     * 執行批次更新操作。
     *
     * <p>將多筆參數清單轉換為 {@link MapSqlParameterSource} 陣列後，
     * 以單次批次呼叫提升效能。</p>
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   每一筆更新的具名參數 Map 清單
     * @return 各筆更新受影響的資料列數陣列
     */
    public int[] updateBatch(ResourceEnum resource, List<Map<String, Object>> params) {
        String sql = getSqlText(resource);
        MapSqlParameterSource[] mapParamArr = params.stream()
                .map(MapSqlParameterSource::new)
                .toArray(MapSqlParameterSource[]::new);
        return support.getNamedParameterJdbcTemplate().batchUpdate(sql, mapParamArr);
    }

    /**
     * 查詢單筆 Bean 物件，不帶參數。
     *
     * @param <T>      回傳 Bean 的型別
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param clazz    回傳 Bean 的 Class，欄位透過 {@link BeanPropertyRowMapper} 自動映射
     * @return 查詢結果的 Bean 物件；若查無資料則回傳 {@code null}
     */
    public <T> T queryForBean(ResourceEnum resource, Class<T> clazz) {
        return queryForBean(resource, new HashMap<>(), null, clazz);
    }

    /**
     * 查詢單筆 Bean 物件，帶動態查詢條件。
     *
     * @param <T>        回傳 Bean 的型別
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param conditions 動態查詢條件，由 {@link Conditions} 建構並注入 WHERE 子句
     * @param clazz      回傳 Bean 的 Class
     * @return 查詢結果的 Bean 物件；若查無資料則回傳 {@code null}
     */
    public <T> T queryForBean(ResourceEnum resource, Conditions conditions, Class<T> clazz) {
        return queryForBean(resource, new HashMap<>(), conditions, clazz);
    }

    /**
     * 查詢單筆 Bean 物件，帶具名參數。
     *
     * @param <T>      回傳 Bean 的型別
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   具名參數 Map
     * @param clazz    回傳 Bean 的 Class
     * @return 查詢結果的 Bean 物件；若查無資料則回傳 {@code null}
     */
    public <T> T queryForBean(ResourceEnum resource, Map<String, Object> params, Class<T> clazz) {
        return queryForBean(resource, params, null, clazz);
    }

    /**
     * 查詢單筆 Bean 物件，帶具名參數與動態查詢條件（完整版）。
     *
     * @param <T>        回傳 Bean 的型別
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param params     具名參數 Map
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @param clazz      回傳 Bean 的 Class
     * @return 查詢結果的 Bean 物件；若查無資料則回傳 {@code null}
     */
    public <T> T queryForBean(ResourceEnum resource, Map<String, Object> params, Conditions conditions, Class<T> clazz) {
        String sql = getSqlText(resource, conditions);
        List<T> results = support.getNamedParameterJdbcTemplate().query(sql, mergeParams(params, conditions), new BeanPropertyRowMapper<>(clazz));
        // 取第一筆，避免呼叫端處理空集合
        return CollectionUtils.isEmpty(results) ? null : results.get(0);
    }

    /**
     * 查詢單一純量或簡單型別的值。
     *
     * <p>適用於 {@code SELECT COUNT(*)} 等回傳單一欄位的情境。</p>
     *
     * @param <T>      回傳值的型別
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   具名參數 Map
     * @param clazz    回傳值的 Class（如 {@code Integer.class}、{@code String.class}）
     * @return 查詢結果的純量值
     */
    public <T> T queryForObject(ResourceEnum resource, Map<String, Object> params, Class<T> clazz) {
        String sql = getSqlText(resource);
        return support.getNamedParameterJdbcTemplate().queryForObject(sql, params, clazz);
    }

    /**
     * 查詢單筆資料並以 {@code Map<String, Object>} 回傳，不帶參數。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @return 以欄位名稱為鍵的 Map；若查無資料則回傳 {@code null}
     */
    public Map<String, Object> queryForMap(ResourceEnum resource) {
        return queryForMap(resource, new HashMap<>(), null);
    }

    /**
     * 查詢單筆資料並以 {@code Map<String, Object>} 回傳，帶動態查詢條件。
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param conditions 動態查詢條件
     * @return 以欄位名稱為鍵的 Map；若查無資料則回傳 {@code null}
     */
    public Map<String, Object> queryForMap(ResourceEnum resource, Conditions conditions) {
        return queryForMap(resource, new HashMap<>(), conditions);
    }

    /**
     * 查詢單筆資料並以 {@code Map<String, Object>} 回傳，帶具名參數。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   具名參數 Map
     * @return 以欄位名稱為鍵的 Map；若查無資料則回傳 {@code null}
     */
    public Map<String, Object> queryForMap(ResourceEnum resource, Map<String, Object> params) {
        return queryForMap(resource, params, null);
    }

    /**
     * 查詢單筆資料並以 {@code Map<String, Object>} 回傳，帶具名參數與動態查詢條件（完整版）。
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param params     具名參數 Map
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @return 以欄位名稱為鍵的 Map；若查無資料則回傳 {@code null}
     */
    public Map<String, Object> queryForMap(ResourceEnum resource, Map<String, Object> params, Conditions conditions) {
        String sql = getSqlText(resource, conditions);
        List<Map<String, Object>> results = support.getNamedParameterJdbcTemplate().queryForList(sql, mergeParams(params, conditions));
        // 取第一筆，避免呼叫端處理空集合
        return CollectionUtils.isEmpty(results) ? null : results.get(0);
    }

    /**
     * 查詢多筆資料並以 {@code List<Map<String, Object>>} 回傳，不帶參數。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @return 查詢結果清單；若查無資料則回傳空清單
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource) {
        return queryForList(resource, new HashMap<>(), null, null);
    }

    /**
     * 查詢多筆資料並以 {@code List<Map<String, Object>>} 回傳，帶動態查詢條件。
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param conditions 動態查詢條件
     * @return 查詢結果清單；若查無資料則回傳空清單
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Conditions conditions) {
        return queryForList(resource, new HashMap<>(), conditions, null);
    }

    /**
     * 查詢多筆資料並以 {@code List<Map<String, Object>>} 回傳，帶具名參數。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   具名參數 Map
     * @return 查詢結果清單；若查無資料則回傳空清單
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Map<String, Object> params) {
        return queryForList(resource, params, null, null);
    }

    /**
     * 查詢多筆資料並以 {@code List<Map<String, Object>>} 回傳，帶具名參數與動態查詢條件。
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param params     具名參數 Map
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @return 查詢結果清單；若查無資料則回傳空清單
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Map<String, Object> params, Conditions conditions) {
        return queryForList(resource, params, conditions, null);
    }

    /**
     * 查詢多筆資料並以 {@code List<Map<String, Object>>} 回傳，帶具名參數、動態查詢條件與分頁（完整版）。
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param params     具名參數 Map
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @param paging     分頁資訊（可為 {@code null}）
     * @return 查詢結果清單；若查無資料則回傳空清單
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Map<String, Object> params, Conditions conditions, Paging paging) {
        String sql = getSqlText(resource, conditions, paging);
        List<Map<String, Object>> results = support.getNamedParameterJdbcTemplate().queryForList(sql, mergeParams(params, conditions));
        return CollectionUtils.isEmpty(results) ? Collections.emptyList() : results;
    }

    /**
     * 查詢多筆 Bean 物件清單，不帶參數。
     *
     * @param <T>      Bean 的型別
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param clazz    Bean 的 Class
     * @return Bean 物件清單；若查無資料則回傳空清單
     */
    public <T> List<T> queryForList(ResourceEnum resource, Class<T> clazz) {
        return queryForList(resource, new HashMap<>(), null, null, clazz);
    }

    /**
     * 查詢多筆 Bean 物件清單，帶分頁。
     *
     * @param <T>      Bean 的型別
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param paging   分頁資訊
     * @param clazz    Bean 的 Class
     * @return Bean 物件清單；若查無資料則回傳空清單
     */
    public <T> List<T> queryForList(ResourceEnum resource, Paging paging, Class<T> clazz) {
        return queryForList(resource, new HashMap<>(), null, paging, clazz);
    }

    /**
     * 查詢多筆 Bean 物件清單，帶動態查詢條件。
     *
     * @param <T>        Bean 的型別
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param conditions 動態查詢條件
     * @param clazz      Bean 的 Class
     * @return Bean 物件清單；若查無資料則回傳空清單
     */
    public <T> List<T> queryForList(ResourceEnum resource, Conditions conditions, Class<T> clazz) {
        return queryForList(resource, new HashMap<>(), conditions, null, clazz);
    }

    /**
     * 查詢多筆 Bean 物件清單，帶具名參數。
     *
     * @param <T>      Bean 的型別
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @param params   具名參數 Map
     * @param clazz    Bean 的 Class
     * @return Bean 物件清單；若查無資料則回傳空清單
     */
    public <T> List<T> queryForList(ResourceEnum resource, Map<String, Object> params, Class<T> clazz) {
        return queryForList(resource, params, null, null, clazz);
    }

    /**
     * 查詢多筆 Bean 物件清單，帶具名參數、動態查詢條件與分頁（完整版）。
     *
     * @param <T>        Bean 的型別
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param params     具名參數 Map
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @param paging     分頁資訊（可為 {@code null}）
     * @param clazz      Bean 的 Class
     * @return Bean 物件清單；若查無資料則回傳空清單
     */
    public <T> List<T> queryForList(ResourceEnum resource, Map<String, Object> params, Conditions conditions, Paging paging, Class<T> clazz) {
        String sql = getSqlText(resource, conditions, paging);
        List<T> results = support.getNamedParameterJdbcTemplate().query(sql, mergeParams(params, conditions), new BeanPropertyRowMapper<>(clazz));
        return CollectionUtils.isEmpty(results) ? Collections.emptyList() : results;
    }

    /**
     * 合併呼叫端參數與 Conditions 收集到的具名參數。
     *
     * <p>Conditions 內的條件值以具名參數佔位符（:c0、:c1...）組裝，實際值存放於
     * {@link Conditions#getParameters()}，必須在執行查詢前併入參數來源，
     * 由 NamedParameterJdbcTemplate 綁定以避免 SQL Injection。</p>
     *
     * @param params     呼叫端傳入的參數
     * @param conditions 查詢條件（可為 null）
     * @return 合併後的參數 Map
     */
    private Map<String, Object> mergeParams(Map<String, Object> params, Conditions conditions) {
        if (conditions == null || conditions.getParameters().isEmpty()) {
            return params;
        }
        Map<String, Object> merged = new HashMap<>(params);
        merged.putAll(conditions.getParameters());
        return merged;
    }

    /**
     * 取得 SQL 文字（不帶條件與分頁的捷徑版本）。
     *
     * @param resource SQL 資源列舉，用於定位 SQL 檔案
     * @return SQL 文字字串
     */
    private String getSqlText(ResourceEnum resource) {
        return getSqlText(resource, null, null);
    }

    /**
     * 取得 SQL 文字並套用動態查詢條件（不帶分頁的捷徑版本）。
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @return 套用條件後的 SQL 文字字串
     */
    private String getSqlText(ResourceEnum resource, Conditions conditions) {
        return getSqlText(resource, conditions, null);
    }

    /**
     * 取得 SQL 文字並套用動態查詢條件與分頁（完整版，含快取）。
     *
     * <p>快取鍵由資源識別碼、conditions hashCode 與 paging hashCode 組成，
     * 確保相同參數組合只讀取一次 SQL 檔案。</p>
     *
     * @param resource   SQL 資源列舉，用於定位 SQL 檔案
     * @param conditions 動態查詢條件（可為 {@code null}）
     * @param paging     分頁資訊（可為 {@code null}）
     * @return 套用條件與分頁後的 SQL 文字字串
     */
    private String getSqlText(ResourceEnum resource, Conditions conditions, Paging paging) {
        // 以資源識別碼、conditions 與 paging 的 hashCode 組合為快取鍵，
        // 相同查詢樣板（不同參數值）可安全共用快取
        String cacheKey = resource.toString() + resource.fileName() + (conditions != null ? conditions.hashCode() : "") + (paging != null ? paging.hashCode() : "");
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            try {
                String sqlPath = resource.dir() + resource.file() + resource.extension();
                String sqlText;

                // 先嘗試從外部文件系統讀取，讓運維人員可不重新部署即修改 SQL
                if (Files.exists(Paths.get(sqlPath))) {
                    sqlText = Files.readString(Paths.get(sqlPath));
                    log.info("Loaded SQL from file system: {}", sqlPath);
                } else {
                    // 外部檔案不存在時，退回從 classpath（JAR 內）讀取
                    File sqlFile = FileUtil.getFileFromClasspath(sqlPath);
                    if (Objects.isNull(sqlFile)) {
                        throw new DataAccessException("SQL file not found: " + sqlPath) {
                        };
                    }
                    sqlText = FileUtil.readFileToString(sqlFile, StandardCharsets.UTF_8.name());
                    log.info("Loaded SQL from classpath: {}", sqlPath);
                }

                if (StringUtils.isBlank(sqlText)) {
                    throw new DataAccessException("SQL file is empty: " + sqlPath) {
                    };
                }

                if (conditions != null) {
                    sqlText = conditions.done(sqlText);
                }

                if (paging != null) {
                    sqlText = applyPaging(sqlText, paging);
                }

                return sqlText;
            } catch (IOException e) {
                log.error("Failed to read SQL file: {}", e.getMessage());
                throw new DataAccessException("Error reading SQL file", e) {
                };
            }
        });
    }

    /**
     * 將分頁資訊套用至 SQL 文字。
     *
     * <p>以 {@link Paging#getPagingSQL()} 提供的範本替換 {@code ${START}}、
     * {@code ${ENDED}}、{@code ${ORDER_BY}}、{@code ${QUERY_STRING}} 四個佔位符，
     * 產生特定資料庫方言的分頁 SQL（例如 Oracle ROWNUM 或 SQL Server ROW_NUMBER()）。</p>
     *
     * @param sqlText 原始 SQL 文字
     * @param paging  分頁資訊
     * @return 套用分頁後的 SQL 文字；若排序欄位為空則直接回傳原始 SQL 並記錄警告
     */
    private String applyPaging(String sqlText, Paging paging) {
        String template = paging.getPagingSQL();
        String orderBy = StringUtils.join(paging.getOrderBy(), ",");

        // 分頁查詢必須指定排序欄位以確保結果集穩定，否則回傳原始 SQL 並警告
        if (StringUtils.isBlank(orderBy)) {
            log.warn("Order by clause is empty for paging query");
            return sqlText;
        }

        // 計算本頁起始列號（1-based）與結束列號，供範本佔位符替換
        String start = String.valueOf((paging.getPage() - 1) * paging.getPagesize() + 1);
        String end = String.valueOf(paging.getPage() * paging.getPagesize());

        return template.replace("${START}", start)
                .replace("${ENDED}", end)
                .replace("${ORDER_BY}", orderBy)
                .replace("${QUERY_STRING}", sqlText);
    }
}
