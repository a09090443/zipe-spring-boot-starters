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

@Slf4j
public abstract class BaseJDBC {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected NamedParameterJdbcDaoSupport support;

    private static final Map<String, String> SQL_CACHE = new ConcurrentHashMap<>();

    /**
     * 執行更新操作，不帶參數
     *
     * @param resource SQL資源枚舉
     * @return 影響的行數
     */
    public int update(ResourceEnum resource) {
        return update(resource, new HashMap<>());
    }

    /**
     * 執行帶參數的更新操作
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數
     * @return 影響的行數
     */
    public int update(ResourceEnum resource, Map<String, Object> params) {
        String sql = getSqlText(resource);
        return support.getNamedParameterJdbcTemplate().update(sql, params);
    }

    /**
     * 執行批量更新操作
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數列表
     * @return 每個更新操作影響的行數陣列
     */
    public int[] updateBatch(ResourceEnum resource, List<Map<String, Object>> params) {
        String sql = getSqlText(resource);
        MapSqlParameterSource[] mapParamArr = params.stream()
                .map(MapSqlParameterSource::new)
                .toArray(MapSqlParameterSource[]::new);
        return support.getNamedParameterJdbcTemplate().batchUpdate(sql, mapParamArr);
    }

    /**
     * 查詢單個Bean對象，不帶參數
     *
     * @param resource SQL資源枚舉
     * @param clazz    返回的Bean類型
     * @return Bean對象，如果查詢結果為空則返回null
     */
    public <T> T queryForBean(ResourceEnum resource, Class<T> clazz) {
        return queryForBean(resource, new HashMap<>(), null, clazz);
    }

    /**
     * 查詢單個Bean對象，帶條件
     *
     * @param resource   SQL資源枚舉
     * @param conditions 查詢條件
     * @param clazz      返回的Bean類型
     * @return Bean對象，如果查詢結果為空則返回null
     */
    public <T> T queryForBean(ResourceEnum resource, Conditions conditions, Class<T> clazz) {
        return queryForBean(resource, new HashMap<>(), conditions, clazz);
    }

    /**
     * 查詢單個Bean對象，帶參數
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數
     * @param clazz    返回的Bean類型
     * @return Bean對象，如果查詢結果為空則返回null
     */
    public <T> T queryForBean(ResourceEnum resource, Map<String, Object> params, Class<T> clazz) {
        return queryForBean(resource, params, null, clazz);
    }

    /**
     * 查詢單個Bean對象，帶參數和條件
     *
     * @param resource   SQL資源枚舉
     * @param params     SQL參數
     * @param conditions 查詢條件
     * @param clazz      返回的Bean類型
     * @return Bean對象，如果查詢結果為空則返回null
     */
    public <T> T queryForBean(ResourceEnum resource, Map<String, Object> params, Conditions conditions, Class<T> clazz) {
        String sql = getSqlText(resource, conditions);
        List<T> results = support.getNamedParameterJdbcTemplate().query(sql, params, new BeanPropertyRowMapper<>(clazz));
        return CollectionUtils.isEmpty(results) ? null : results.get(0);
    }

    /**
     * 查詢單個對象
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數
     * @param clazz    返回的對象類型
     * @return 查詢結果對象
     */
    public <T> T queryForObject(ResourceEnum resource, Map<String, Object> params, Class<T> clazz) {
        String sql = getSqlText(resource);
        return support.getNamedParameterJdbcTemplate().queryForObject(sql, params, clazz);
    }

    /**
     * 查詢單個Map對象，不帶參數
     *
     * @param resource SQL資源枚舉
     * @return Map對象，如果查詢結果為空則返回null
     */
    public Map<String, Object> queryForMap(ResourceEnum resource) {
        return queryForMap(resource, new HashMap<>(), null);
    }

    /**
     * 查詢單個Map對象，帶條件
     *
     * @param resource   SQL資源枚舉
     * @param conditions 查詢條件
     * @return Map對象，如果查詢結果為空則返回null
     */
    public Map<String, Object> queryForMap(ResourceEnum resource, Conditions conditions) {
        return queryForMap(resource, new HashMap<>(), conditions);
    }

    /**
     * 查詢單個Map對象，帶參數
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數
     * @return Map對象，如果查詢結果為空則返回null
     */
    public Map<String, Object> queryForMap(ResourceEnum resource, Map<String, Object> params) {
        return queryForMap(resource, params, null);
    }

    /**
     * 查詢單個Map對象，帶參數和條件
     *
     * @param resource   SQL資源枚舉
     * @param params     SQL參數
     * @param conditions 查詢條件
     * @return Map對象，如果查詢結果為空則返回null
     */
    public Map<String, Object> queryForMap(ResourceEnum resource, Map<String, Object> params, Conditions conditions) {
        String sql = getSqlText(resource, conditions);
        List<Map<String, Object>> results = support.getNamedParameterJdbcTemplate().queryForList(sql, params);
        return CollectionUtils.isEmpty(results) ? null : results.get(0);
    }

    /**
     * 查詢Map列表，不帶參數
     *
     * @param resource SQL資源枚舉
     * @return Map列表，如果查詢結果為空則返回空列表
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource) {
        return queryForList(resource, new HashMap<>(), null, null);
    }

    /**
     * 查詢Map列表，帶條件
     *
     * @param resource   SQL資源枚舉
     * @param conditions 查詢條件
     * @return Map列表，如果查詢結果為空則返回空列表
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Conditions conditions) {
        return queryForList(resource, new HashMap<>(), conditions, null);
    }

    /**
     * 查詢Map列表，帶參數
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數
     * @return Map列表，如果查詢結果為空則返回空列表
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Map<String, Object> params) {
        return queryForList(resource, params, null, null);
    }

    /**
     * 查詢Map列表，帶參數和條件
     *
     * @param resource   SQL資源枚舉
     * @param params     SQL參數
     * @param conditions 查詢條件
     * @return Map列表，如果查詢結果為空則返回空列表
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Map<String, Object> params, Conditions conditions) {
        return queryForList(resource, params, conditions, null);
    }

    /**
     * 查詢Map列表，帶參數、條件和分頁
     *
     * @param resource   SQL資源枚舉
     * @param params     SQL參數
     * @param conditions 查詢條件
     * @param paging     分頁信息
     * @return Map列表，如果查詢結果為空則返回空列表
     */
    public List<Map<String, Object>> queryForList(ResourceEnum resource, Map<String, Object> params, Conditions conditions, Paging paging) {
        String sql = getSqlText(resource, conditions, paging);
        List<Map<String, Object>> results = support.getNamedParameterJdbcTemplate().queryForList(sql, params);
        return CollectionUtils.isEmpty(results) ? Collections.emptyList() : results;
    }

    /**
     * 查詢Bean列表，不帶參數
     *
     * @param resource SQL資源枚舉
     * @param clazz    返回的Bean類型
     * @return Bean列表，如果查詢結果為空則返回空列表
     */
    public <T> List<T> queryForList(ResourceEnum resource, Class<T> clazz) {
        return queryForList(resource, new HashMap<>(), null, null, clazz);
    }

    /**
     * 查詢Bean列表，帶分頁
     *
     * @param resource SQL資源枚舉
     * @param paging   分頁信息
     * @param clazz    返回的Bean類型
     * @return Bean列表，如果查詢結果為空則返回空列表
     */
    public <T> List<T> queryForList(ResourceEnum resource, Paging paging, Class<T> clazz) {
        return queryForList(resource, new HashMap<>(), null, paging, clazz);
    }

    /**
     * 查詢Bean列表，帶條件
     *
     * @param resource   SQL資源枚舉
     * @param conditions 查詢條件
     * @param clazz      返回的Bean類型
     * @return Bean列表，如果查詢結果為空則返回空列表
     */
    public <T> List<T> queryForList(ResourceEnum resource, Conditions conditions, Class<T> clazz) {
        return queryForList(resource, new HashMap<>(), conditions, null, clazz);
    }

    /**
     * 查詢Bean列表，帶參數
     *
     * @param resource SQL資源枚舉
     * @param params   SQL參數
     * @param clazz    返回的Bean類型
     * @return Bean列表，如果查詢結果為空則返回空列表
     */
    public <T> List<T> queryForList(ResourceEnum resource, Map<String, Object> params, Class<T> clazz) {
        return queryForList(resource, params, null, null, clazz);
    }

    /**
     * 查詢Bean列表，帶參數、條件和分頁
     *
     * @param resource   SQL資源枚舉
     * @param params     SQL參數
     * @param conditions 查詢條件
     * @param paging     分頁信息
     * @param clazz      返回的Bean類型
     * @return Bean列表，如果查詢結果為空則返回空列表
     */
    public <T> List<T> queryForList(ResourceEnum resource, Map<String, Object> params, Conditions conditions, Paging paging, Class<T> clazz) {
        String sql = getSqlText(resource, conditions, paging);
        List<T> results = support.getNamedParameterJdbcTemplate().query(sql, params, new BeanPropertyRowMapper<>(clazz));
        return CollectionUtils.isEmpty(results) ? Collections.emptyList() : results;
    }

    /**
     * 獲取SQL文本
     *
     * @param resource SQL資源枚舉
     * @return SQL文本
     */
    private String getSqlText(ResourceEnum resource) {
        return getSqlText(resource, null, null);
    }

    /**
     * 獲取SQL文本，帶條件
     *
     * @param resource   SQL資源枚舉
     * @param conditions 查詢條件
     * @return SQL文本
     */
    private String getSqlText(ResourceEnum resource, Conditions conditions) {
        return getSqlText(resource, conditions, null);
    }

    /**
     * 獲取SQL文本，帶條件和分頁
     *
     * @param resource   SQL資源枚舉
     * @param conditions 查詢條件
     * @param paging     分頁信息
     * @return SQL文本
     */
    private String getSqlText(ResourceEnum resource, Conditions conditions, Paging paging) {
        String cacheKey = resource.toString() + resource.fileName() + (conditions != null ? conditions.hashCode() : "") + (paging != null ? paging.hashCode() : "");
        return SQL_CACHE.computeIfAbsent(cacheKey, k -> {
            try {
                String sqlPath = resource.dir() + resource.file() + resource.extension();
                String sqlText;

                // 先嘗試從外部文件系統讀取
                if (Files.exists(Paths.get(sqlPath))) {
                    sqlText = Files.readString(Paths.get(sqlPath));
                    log.info("Loaded SQL from file system: {}", sqlPath);
                } else {
                    // 如果外部文件不存在，則嘗試從 classpath 讀取
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
     * 應用分頁邏輯到SQL
     *
     * @param sqlText 原始SQL文本
     * @param paging  分頁信息
     * @return 應用分頁後的SQL文本
     */
    private String applyPaging(String sqlText, Paging paging) {
        String template = paging.getPagingSQL();
        String orderBy = StringUtils.join(paging.getOrderBy(), ",");

        if (StringUtils.isBlank(orderBy)) {
            log.warn("Order by clause is empty for paging query");
            return sqlText;
        }

        String start = String.valueOf((paging.getPage() - 1) * paging.getPagesize() + 1);
        String end = String.valueOf(paging.getPage() * paging.getPagesize());

        return template.replace("${START}", start)
                .replace("${ENDED}", end)
                .replace("${ORDER_BY}", orderBy)
                .replace("${QUERY_STRING}", sqlText);
    }
}
