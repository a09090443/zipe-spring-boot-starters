package com.zipe.jdbc.criteria;

/**
 * 記錄資料庫的操作語句常數。
 * <p>
 * 封裝常用的 SQL 運算子與關鍵字，供查詢條件建構時以列舉方式統一引用，
 * 避免在程式中直接散落字串字面值，提升可維護性與可讀性。
 * </p>
 *
 * @author adam.yeh
 */
public enum SQL {

    /** 邏輯 OR 運算子 */
    OR("OR"),
    /** IN 範圍比對運算子 */
    IN("IN"),
    /** NOT IN 排除範圍運算子 */
    NOTIN("NOT IN"),
    /** 大於（Greater Than）比較運算子 */
    GT(">"),
    /** 小於（Less Than）比較運算子 */
    LT("<"),
    /** 邏輯 AND 運算子 */
    AND("AND"),
    /** 等於比較運算子 */
    EQUAL("="),
    /** 大於等於比較運算子 */
    GTEQUAL(">="),
    /** 小於等於比較運算子 */
    LTEQUAL("<="),
    /** 升冪排序關鍵字 */
    ASC("ASC"),
    /** 降冪排序關鍵字 */
    DESC("DESC"),
    /** 模糊比對運算子 */
    LIKE("LIKE"),
    /** 不等於比較運算子 */
    UNEQUAL("<>"),
    /** 判斷欄位值為 NULL */
    ISNULL("IS NULL"),
    /** 判斷欄位值不為 NULL */
    NOTNULL("IS NOT NULL"),
    /** 子查詢不存在條件 */
    NOTEXISTS("NOT EXISTS");

    /** 對應的 SQL 運算子字串 */
    private String operator;

    /**
     * 以指定的 SQL 運算子字串初始化列舉常數。
     *
     * @param o SQL 運算子字串，例如 {@code "AND"}、{@code ">"}
     */
    private SQL (String o) {
        this.operator = o;
    }

    /**
     * 取得此列舉常數對應的 SQL 運算子字串。
     *
     * @return SQL 運算子字串，例如 {@code "AND"}、{@code ">"}
     */
    public String operator () {
        return this.operator;
    }

}
