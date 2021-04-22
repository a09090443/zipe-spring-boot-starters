package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/4/22 上午 10:02
 **/
@Configuration
@ConfigurationProperties(prefix = "web")
@Data
public class WebPropertyConfig {

    private JspConfig jsp;
    private ThymeleafConfig thymeleaf;
}
