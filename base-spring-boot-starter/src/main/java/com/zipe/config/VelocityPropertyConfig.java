package com.zipe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/3/12 下午 02:01
 **/
@Configuration
@ConfigurationProperties(prefix = "template")
@Data
public class VelocityPropertyConfig {

    private String path;

}
