package com.zipe.jdbc.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Conditions 以具名參數方式組裝查詢條件，避免 SQL Injection。
 */
class ConditionsTest {

    /**
     * equal 不可將使用者輸入直接拼進 SQL，必須改用具名參數佔位符，
     * 並把實際值收集到 parameters 供 NamedParameterJdbcTemplate 綁定。
     */
    @Test
    void equalUsesNamedParameterInsteadOfConcatenatingValue() {
        String injection = "x' OR '1'='1";
        Conditions conditions = new Conditions().equal("name", injection);

        String sql = conditions.done("SELECT * FROM t WHERE ${CONDITIONS}");
        Map<String, Object> params = conditions.getParameters();

        // SQL 不可包含原始注入字串
        assertFalse(sql.contains(injection), "SQL 不應包含原始輸入值: " + sql);
        // 必須使用具名參數佔位符
        assertTrue(sql.matches(".*name\\s*=\\s*:\\w+.*"), "應使用具名參數佔位符: " + sql);
        // 參數值需被收集
        assertTrue(params.containsValue(injection), "參數應包含原始輸入值");
    }

    /**
     * like 必須參數化，且把萬用字元包進參數值而非 SQL 字串。
     */
    @Test
    void likeUsesNamedParameterWithWildcardInValue() {
        Conditions conditions = new Conditions().like("title", "abc");

        String sql = conditions.done("SELECT * FROM t WHERE ${CONDITIONS}");
        Map<String, Object> params = conditions.getParameters();

        assertTrue(sql.matches(".*title\\s+LIKE\\s+:\\w+.*"), "like 應使用具名參數: " + sql);
        assertTrue(params.containsValue("%abc%"), "萬用字元應包進參數值");
    }

    /**
     * in 條件需為每個值產生獨立的具名參數，不可字串拼接。
     */
    @Test
    void inUsesNamedParametersForEachValue() {
        Conditions conditions = new Conditions().in("id", Arrays.asList("1", "2'); DROP TABLE t;--"));

        String sql = conditions.done("SELECT * FROM t WHERE ${CONDITIONS}");
        Map<String, Object> params = conditions.getParameters();

        assertFalse(sql.contains("DROP TABLE"), "SQL 不應包含原始注入字串: " + sql);
        assertEquals(2, params.size(), "in 應為每個值各產生一個參數");
        assertTrue(params.containsValue("2'); DROP TABLE t;--"));
    }

    /**
     * 欄位名稱無法參數化，必須以白名單驗證，非法字元應丟出例外。
     */
    @Test
    void rejectsIllegalColumnName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Conditions().equal("name; DROP TABLE t;--", "x"));
    }

    /**
     * orderBy 欄位同樣需驗證，非法字元應丟出例外。
     */
    @Test
    void rejectsIllegalOrderByColumn() {
        assertThrows(IllegalArgumentException.class,
                () -> new Conditions().orderBy("1; DROP TABLE t;--"));
    }
}
