package com.zipe.base.model;

import lombok.Data;

@Data
public class DynamicDataSourceConfig {
    private String name;
    private String url;
    private String username;
    private String pa55word;
    private String driverClassName;
}
