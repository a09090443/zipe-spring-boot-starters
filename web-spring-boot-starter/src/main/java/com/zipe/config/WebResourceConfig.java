package com.zipe.config;

import lombok.Data;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 11:25
 **/
@Data
public class WebResourceConfig {

    private String pathPattern = "/static/**";
    private String location = "/WEB-INF/static/";

}
