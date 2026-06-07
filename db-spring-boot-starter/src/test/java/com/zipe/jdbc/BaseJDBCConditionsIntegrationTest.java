package com.zipe.jdbc;

import com.zipe.enums.ResourceEnum;
import com.zipe.jdbc.criteria.Conditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 以 H2 記憶體資料庫驗證 BaseJDBC 搭配 Conditions 時，
 * 條件值會被正確綁定為具名參數，且 SQL Injection 嘗試無效。
 */
class BaseJDBCConditionsIntegrationTest {

    private TestUserDao dao;

    /** 測試專用 DAO，直接注入 H2 的 JdbcTemplate 與 support。 */
    static class TestUserDao extends BaseJDBC {
        TestUserDao(JdbcTemplate jdbcTemplate, NamedParameterJdbcDaoSupport support) {
            this.jdbcTemplate = jdbcTemplate;
            this.support = support;
        }

        List<Map<String, Object>> findByName(Conditions conditions) {
            return queryForList(ResourceEnum.SQL.getResource("findUsers"), new HashMap<>(), conditions);
        }
    }

    @BeforeEach
    void setUp() {
        DataSource dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(), "jdbc:h2:mem:condtest;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("CREATE TABLE users (id INT, name VARCHAR(50))");
        jdbcTemplate.update("INSERT INTO users VALUES (1, 'alice')");
        jdbcTemplate.update("INSERT INTO users VALUES (2, 'bob')");

        NamedParameterJdbcDaoSupport support = new NamedParameterJdbcDaoSupport();
        support.setJdbcTemplate(jdbcTemplate);

        dao = new TestUserDao(jdbcTemplate, support);
    }

    @Test
    void conditionValueIsBoundAsNamedParameter() {
        List<Map<String, Object>> result = dao.findByName(new Conditions().equal("name", "alice"));

        assertEquals(1, result.size(), "應只回傳 alice 一筆");
        assertEquals("alice", String.valueOf(result.get(0).get("NAME")));
    }

    @Test
    void sqlInjectionAttemptReturnsNoRows() {
        // 經典 tautology 注入；若被正確參數化，會被當成一個不存在的 name 值，回傳 0 筆
        List<Map<String, Object>> result =
                dao.findByName(new Conditions().equal("name", "' OR '1'='1"));

        assertTrue(result.isEmpty(), "注入字串應被當作參數值，不得回傳全部資料列");
    }
}
