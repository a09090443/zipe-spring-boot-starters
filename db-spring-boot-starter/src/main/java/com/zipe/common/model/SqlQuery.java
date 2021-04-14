package com.zipe.common.model;

import com.zipe.jdbc.criteria.Conditions;
import lombok.Data;

import java.util.Map;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/10 下午 04:44
 **/
@Data
public class SqlQuery<T> {

    private String sqlDir;
    private String sqlFileName;
    private Class<T> clazz;
    private Conditions conditions;
    private Map<String, Object> params;

    public SqlQuery(String sqlFileName, Class<T> clazz, Conditions conditions, Map<String, Object> params) {
        this.sqlFileName = sqlFileName;
        this.clazz = clazz;
        this.conditions = conditions;
        this.params = params;
    }

    public SqlQuery(String sqlDir, String sqlFileName, Class<T> clazz, Conditions conditions, Map<String, Object> params) {
        this.sqlDir = sqlDir;
        this.sqlFileName = sqlFileName;
        this.clazz = clazz;
        this.conditions = conditions;
        this.params = params;
    }

}
