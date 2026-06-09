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

    /**
     * 建立一個空的查詢條件物件，初始化內部 SQL 片段緩衝區。
     */
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

    /** 產生下一個唯一的具名參數名稱（格式：c0, c1, c2 ...）。 */
    private String nextParamName () {
        return "c" + (paramIndex++);
    }

    /**
     * 將值登錄至參數 Map，並回傳對應的具名參數名稱，
     * 供後續在 SQL 片段中以 {@code :name} 形式引用。
     */
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

    /**
     * 新增等於（=）條件。
     *
     * @param column 欄位名稱
     * @param value  比對值
     * @return 本物件（支援 method chaining）
     */
    public Conditions equal (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.EQUAL));
        return this;
    }

    /**
     * 新增模糊比對（LIKE）條件，值會自動加上前後 % 萬用字元。
     *
     * @param column 欄位名稱
     * @param value  比對關鍵字
     * @return 本物件（支援 method chaining）
     */
    public Conditions like (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.LIKE));
        return this;
    }

    /**
     * 新增不等於（!=）條件。
     *
     * @param column 欄位名稱
     * @param value  比對值
     * @return 本物件（支援 method chaining）
     */
    public Conditions unEqual (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.UNEQUAL));
        return this;
    }

    /**
     * 新增 IN 條件，比對欄位值是否存在於指定清單中。
     *
     * @param column 欄位名稱
     * @param values 允許的值清單
     * @return 本物件（支援 method chaining）
     */
    public Conditions in (String column, List<String> values) {
        appendPairTypes(new Pair(column, values, SQL.IN));
        return this;
    }

    /**
     * 新增 NOT IN 條件，比對欄位值是否不存在於指定清單中。
     *
     * @param column 欄位名稱
     * @param values 排除的值清單
     * @return 本物件（支援 method chaining）
     */
    public Conditions notIn (String column, List<String> values) {
        appendPairTypes(new Pair(column, values, SQL.NOTIN));
        return this;
    }

    /**
     * 新增大於（&gt;）條件。
     *
     * @param column 欄位名稱
     * @param value  比對值
     * @return 本物件（支援 method chaining）
     */
    public Conditions gt (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.GT));
        return this;
    }

    /**
     * 新增小於（&lt;）條件。
     *
     * @param column 欄位名稱
     * @param value  比對值
     * @return 本物件（支援 method chaining）
     */
    public Conditions lt (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.LT));
        return this;
    }

    /**
     * 新增大於等於（&gt;=）條件。
     *
     * @param column 欄位名稱
     * @param value  比對值
     * @return 本物件（支援 method chaining）
     */
    public Conditions gtEqual (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.GTEQUAL));
        return this;
    }

    /**
     * 新增小於等於（&lt;=）條件。
     *
     * @param column 欄位名稱
     * @param value  比對值
     * @return 本物件（支援 method chaining）
     */
    public Conditions ltEqual (String column, String value) {
        appendPairTypes(new Pair(column, value, SQL.LTEQUAL));
        return this;
    }

    /**
     * 新增 IS NOT NULL 條件。
     *
     * @param column 欄位名稱
     * @return 本物件（支援 method chaining）
     */
    public Conditions notNull (String column) {
        appendPairTypes(new Pair(column, SQL.NOTNULL));
        return this;
    }

    /**
     * 新增 IS NULL 條件。
     *
     * @param column 欄位名稱
     * @return 本物件（支援 method chaining）
     */
    public Conditions isNull (String column) {
        appendPairTypes(new Pair(column, SQL.ISNULL));
        return this;
    }

    /**
     * 新增 NOT EXISTS 條件，{@code value} 為子查詢 SQL 字串。
     *
     * <p><b>資安警告：</b>子查詢無法參數化，{@code value} 必須為程式內固定字串，
     * 嚴禁傳入任何使用者可控的輸入。</p>
     *
     * @param value 子查詢 SQL 字串
     * @return 本物件（支援 method chaining）
     */
    public Conditions notExists (String value) {
        appendPairTypes(new Pair("", value, SQL.NOTEXISTS));
        return this;
    }

    /**
     * 在條件字串中插入左括弧，用於群組化子條件。
     *
     * @return 本物件（支援 method chaining）
     */
    public Conditions leftPT () {
        condition.append(" (");
        return this;
    }

    /**
     * 在條件字串中插入「連結符號 + 左括弧」，用於以 AND / OR 開頭的子群組。
     *
     * @param e 連結符號 ( AND、OR... )
     * @return 本物件（支援 method chaining）
     */
    public Conditions leftPT (SQL e) {
        condition.append(" " + e.operator() + " (");
        return this;
    }

    /**
     * 在條件字串中插入右括弧，結束子條件群組。
     *
     * @return 本物件（支援 method chaining）
     */
    public Conditions rightPT () {
        condition.append(") ");
        return this;
    }

    /**
     * 在條件字串中插入「右括弧 + 連結符號」，結束子群組後緊接後續連結。
     *
     * @param e 連結符號 ( AND、OR... )
     * @return 本物件（支援 method chaining）
     */
    public Conditions rightPT (SQL e) {
        condition.append(") " + e.operator() + " ");
        return this;
    }

    /**
     * 將組裝好的條件句參數 AND 起來。
     *
     * @return 本物件（支援 method chaining）
     */
    public Conditions and () {
        condition.append(" " + SQL.AND.operator() + " ");
        return this;
    }

    /**
     * 將組裝好的條件句參數 OR 起來。
     *
     * @return 本物件（支援 method chaining）
     */
    public Conditions or () {
        condition.append(" " + SQL.OR.operator() + " ");
        return this;
    }

    /**
     * 以預設升冪（ASC）對指定欄位排序。
     *
     * @param column 排序欄位名稱
     * @return 本物件（支援 method chaining）
     */
    public Conditions orderBy (String column) {
        return orderBy(column, SQL.ASC);
    }

    /**
     * 對指定欄位以指定方向排序。
     *
     * @param column 排序欄位名稱
     * @param order  排序方向（{@link SQL#ASC} 升冪 / {@link SQL#DESC} 降冪）
     * @return 本物件（支援 method chaining）
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
     * @param sql 要附加的 SQL 語法片段
     * @return 本物件（支援 method chaining）
     */
    public Conditions rawSql (String sql) {
        condition.append(" " + sql);
        return this;
    }

    /**
     * 宣告條件句組裝結束，將條件片段替換進 SQL 範本並回傳最終 SQL 字串。
     *
     * <p>方法執行後內部緩衝區會被清空（設為 {@code null}），物件不可再重用。</p>
     *
     * @param sqlText 含有 {@code ${CONDITIONS}} 佔位符的 SQL 範本字串
     * @return 替換完成後的完整 SQL 字串
     */
    public String done (String sqlText) {
        String done = StringUtils.replace(sqlText, "${CONDITIONS}", condition.toString());
        condition = null;

        return done;
    }

    /**
     * 依據 {@link Pair} 攜帶的條件類型，將對應的 SQL 片段與具名參數追加至內部緩衝區。
     * IN / NOT IN 會逐一展開各個佔位符；LIKE 自動補上前後萬用字元；
     * NOT EXISTS 保留子查詢原文；IS NULL / IS NOT NULL 不需要值。
     */
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
