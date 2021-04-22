package com.zipe.config;

import lombok.Data;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 10:09
 **/
@Data
public class JspConfig {

    private Boolean enable = false;
    private String viewNames = "jsp/*";
    private String stuff = ".jsp";

}
