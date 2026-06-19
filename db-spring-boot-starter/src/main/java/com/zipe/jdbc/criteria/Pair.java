package com.zipe.jdbc.criteria;

import java.util.List;

/**
 * 客製化Condition類別的參數組成資料類別
 *
 * @author adam.yeh
 * @create date: NOV 19, 2017
 */
public class Pair {

    /** 欄位名稱（對應資料庫欄位） */
    private String column;

    /** 單一比對值（用於 = / LIKE / 不等於等單值條件） */
    private String value;

    /** SQL 比對類型，決定此 Pair 產生的 SQL 片段語意 */
    private SQL matchType;

    /** 多值清單（用於 IN / NOT IN 等多值條件） */
    private List<String> values;

    /**
     * 建立僅含欄位與比對類型的 Pair，適用於 IS NULL / IS NOT NULL 等不需傳值的條件。
     *
     * @param column    資料庫欄位名稱
     * @param matchType SQL 比對類型
     */
    public Pair (String column, SQL matchType) {
        this.column = column;
        this.matchType = matchType;
    }

    /**
     * 建立含單一比對值的 Pair，適用於等值、LIKE、範圍等單值條件。
     *
     * @param column    資料庫欄位名稱
     * @param value     比對用的單一字串值
     * @param matchType SQL 比對類型
     */
    public Pair (String column, String value, SQL matchType) {
        this.column = column;
        this.value = value;
        this.matchType = matchType;
    }

    /**
     * 建立含多值清單的 Pair，適用於 IN / NOT IN 等多值條件。
     *
     * @param column    資料庫欄位名稱
     * @param values    比對用的多值字串清單
     * @param matchType SQL 比對類型
     */
    public Pair (String column, List<String> values, SQL matchType) {
        this.column = column;
        this.values = values;
        this.matchType = matchType;
    }

    /**
     * 取得欄位名稱。
     *
     * @return 資料庫欄位名稱
     */
    public String getColumn () {
        return column;
    }

    /**
     * 設定欄位名稱。
     *
     * @param column 資料庫欄位名稱
     */
    public void setColumn (String column) {
        this.column = column;
    }

    /**
     * 取得單一比對值。
     *
     * @return 比對用的單一字串值
     */
    public String getValue () {
        return value;
    }

    /**
     * 設定單一比對值。
     *
     * @param value 比對用的單一字串值
     */
    public void setValue (String value) {
        this.value = value;
    }

    /**
     * 取得 SQL 比對類型。
     *
     * @return SQL 比對類型列舉值
     */
    public SQL getMatchType () {
        return matchType;
    }

    /**
     * 設定 SQL 比對類型。
     *
     * @param matchType SQL 比對類型列舉值
     */
    public void setMatchType (SQL matchType) {
        this.matchType = matchType;
    }

    /**
     * 取得多值比對清單。
     *
     * @return 比對用的多值字串清單
     */
    public List<String> getValues () {
        return values;
    }

    /**
     * 設定多值比對清單。
     *
     * @param values 比對用的多值字串清單
     */
    public void setValues (List<String> values) {
        this.values = values;
    }

}
