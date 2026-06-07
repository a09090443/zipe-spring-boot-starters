package com.zipe.jdbc.criteria;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JDBC DAO的查詢條件物件化類別。
 *
 * <p>所有條件值均以具名參數（named parameter）方式組裝，實際值收集於 {@link #getParameters()}，
 * 由 NamedParameterJdbcTemplate 進行綁定，避免 SQL Injection。欄位名稱無法參數化，
 * 因此一律以白名單規則驗證。</p>
 *
 * @author adam.yeh
 * @create date: NOV 19, 2017
 */
public class Conditions {

    /** 合法欄位名稱規則（僅允許英數、底線與點，用於 schema.table.column）。 */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_.]+$");

    private StringBuilder condition;

    /** 收集條件中所有具名參數的實際值。 */
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    /** 具名參數流水號，確保同一物件內參數名稱唯一且可預期。 */
    private int paramIndex = 0;

    public Conditions () {
        condition = new StringBuilder();
    }

    /**
     * 取得本條件物件收集到的具名參數值。
     *
     * @return 參數名稱對應實際值的 Map
     */
    public Map<String, Object> getParameters () {
        return parameters;
    }

    private String nextParamName () {
        return "c" + (paramIndex++);
    }

    private String bindValue (Object value) {
        String name = nextParamName();
        parameters.put(name, value);
        return name;
    }

    /**
     * 驗證欄位名稱是否合法（欄位名無法參數化，只能以白名單防護）。
     *
     * @param column 欄位名稱
     */
    private static void validateColumn (String column) {
        if (column == null || !SAFE_IDENTIFIER.matcher(column).matches()) {
            throw new IllegalArgumentException("Illegal column name: " + column);
        }
    }

    public Conditions equal (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.EQUAL));
        return this;
    }

    public Conditions like (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.LIKE));
        return this;
    }

    public Conditions unEqual (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.UNEQUAL));
        return this;
    }

    public Conditions in (String column, List<String> values) {
        appendPairTypes(new Pair(column, values, SQL.IN));
        return this;
    }

    public Conditions notIn (String column, List<String> values) {
        appendPairTypes(new Pair(column, values, SQL.NOTIN));
        return this;
    }

    public Conditions gt (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.GT));
        return this;
    }

    public Conditions lt (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.LT));
        return this;
    }

    public Conditions gtEqual (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.GTEQUAL));
        return this;
    }

    public Conditions ltEqual (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.LTEQUAL));
        return this;
    }

    public Conditions notNull (String column) {
        appendPairTypes(new Pair(column, SQL.NOTNULL));
        return this;
    }

    public Conditions isNull (String column) {
        appendPairTypes(new Pair(column, SQL.ISNULL));
        return this;
    }

    public Conditions notExists (String value) {
        appendPairTypes(new Pair("", value, SQL.NOTEXISTS));
        return this;
    }

    /**
     * 左括弧
     *
     * @return
     */
    public Conditions leftPT () {
        condition.append(" (");
        return this;
    }

    /**
     * 左括弧
     *
     * @param e 連結符號 ( AND、OR... )
     * @return
     */

    public Conditions leftPT (SQL e) {
        condition.append(" " + e.operator() + " (");
        return this;
    }

    /**
     * 右括弧
     *
     * @return
     */
    public Conditions rightPT () {
        condition.append(") ");
        return this;
    }

    /**
     * 右括弧
     *
     * @param e 連結符號 ( AND、OR... )
     * @return
     */
    public Conditions rightPT (SQL e) {
        condition.append(") " + e.operator() + " ");
        return this;
    }

    /**
     * 將組裝好的條件句參數AND起來
     *
     * @return
     */
    public Conditions and () {
        condition.append(" " + SQL.AND.operator() + " ");
        return this;
    }

    /**
     * 將組裝好的條件句參數OR起來
     *
     * @return
     */
    public Conditions or () {
        condition.append(" " + SQL.OR.operator() + " ");
        return this;
    }

    /**
     * 排序
     *
     * @return
     */
    public Conditions orderBy (String column) {
        return orderBy(column, SQL.ASC);
    }

    /**
     * 排序
     *
     * @param order 升/降 冪
     * @return
     */
    public Conditions orderBy (String column, SQL order) {
        validateColumn(column);
        condition.append(" ORDER BY " + column + " " + order.operator() + " ");
        return this;
    }

    /**
     * 直接附加原生 SQL 片段。
     *
     * <p><b>資安警告：</b>此方法不做任何跳脫或參數化，傳入內容會原樣拼進 SQL。
     * 嚴禁傳入任何使用者可控的輸入，僅可用於程式內固定字串。</p>
     *
     * @param sql SQL語法
     * @return
     */
    public Conditions rawSql (String sql) {
        condition.append(" " + sql);
        return this;
    }

    /**
     * 宣告條件句結束
     *
     * @return
     */
    public String done (String sqlText) {
        String done = StringUtils.replace(sqlText, "${CONDITIONS}", condition.toString());
        condition = null;

        return done;
    }

    private void appendPairTypes (Pair pair) {
        String column = pair.getColumn();
        String value = pair.getValue();
        SQL type = pair.getMatchType();
        String operator = type.operator();
        List<String> values = pair.getValues();

        switch (type) {
            case IN:
            case NOTIN:
                validateColumn(column);
                StringBuilder placeholders = new StringBuilder("( ");
                for (int i = 0; i < values.size(); i++) {
                    placeholders.append(":").append(bindValue(values.get(i)));
                    if (i < values.size() - 1) {
                        placeholders.append(", ");
                    }
                }
                placeholders.append(" )");
                condition.append(column + " " + operator + " " + placeholders);
                break;

            case LIKE:
                validateColumn(column);
                condition.append(column + " " + operator + " :" + bindValue("%" + value + "%"));
                break;

            case NOTEXISTS:
                // value 為子查詢 SQL，無法參數化；保留原樣，呼叫端不得傳入使用者輸入。
                condition.append(" " + operator + " ( " + value + " ) ");
                break;

            case ISNULL:
            case NOTNULL:
                validateColumn(column);
                condition.append(column + " " + operator);
                break;

            default:
                validateColumn(column);
                condition.append(column + " " + operator + " :" + bindValue(value));
                break;
        }
    }

}
