package com.zipe.base.model;

import lombok.Data;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/3/12 下午 02:12
 **/
@Data
public class DynamicDataSourceConfig {
    private String name;
    private String url;
    private String username;
    private String pa55word;
    private String driverClassName;
}
