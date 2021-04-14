package com.zipe.jdbc.criteria;

/**
 * 記錄資料庫的操作語句常數
 *
 * @author adam.yeh
 */
public enum SQL {

    OR("OR"),
    IN("IN"),
    NOTIN("NOT IN"),
    GT(">"),
    LT("<"),
    AND("AND"),
    EQUAL("="),
    GTEQUAL(">="),
    LTEQUAL("<="),
    ASC("ASC"),
    DESC("DESC"),
    LIKE("LIKE"),
    UNEQUAL("<>"),
    ISNULL("IS NULL"),
    NOTNULL("IS NOT NULL"),
    NOTEXISTS("NOT EXISTS");

    private String operator;

    private SQL (String o) {
        this.operator = o;
    }

    public String operator () {
        return this.operator;
    }

}
