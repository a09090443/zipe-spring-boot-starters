package com.zipe.jdbc.criteria;

import java.util.List;

/**
 * 客製化Condition類別的參數組成資料類別
 *
 * @author adam.yeh
 * @create date: NOV 19, 2017
 */
public class Pair {

    private String column;
    private String value;
    private SQL matchType;
    private List<String> values;

    public Pair (String column, SQL matchType) {
        this.column = column;
        this.matchType = matchType;
    }

    public Pair (String column, String value, SQL matchType) {
        this.column = column;
        this.value = value;
        this.matchType = matchType;
    }

    public Pair (String column, List<String> values, SQL matchType) {
        this.column = column;
        this.values = values;
        this.matchType = matchType;
    }

    public String getColumn () {
        return column;
    }

    public void setColumn (String column) {
        this.column = column;
    }

    public String getValue () {
        return value;
    }

    public void setValue (String value) {
        this.value = value;
    }

    public SQL getMatchType () {
        return matchType;
    }

    public void setMatchType (SQL matchType) {
        this.matchType = matchType;
    }

    public List<String> getValues () {
        return values;
    }

    public void setValues (List<String> values) {
        this.values = values;
    }

}
