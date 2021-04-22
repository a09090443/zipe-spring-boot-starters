package com.zipe.config;

import lombok.Data;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 10:11
 **/
@Data
public class ThymeleafConfig {

    private Boolean enable = true;
    private String viewNames = "html/*,vue/*,templates/*,th/*";
    private String stuff = ".html";
    private String templateMode = "HTML";

}
