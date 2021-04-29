package com.zipe.quartz.autoconfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;

@Configuration
@Order(-1)
@PropertySource(value = {"classpath:quartz-jobs.properties","classpath:quartz-datasource.properties","classpath:spring-quartz.properties"}, encoding = "UTF-8")
public class QuartzPropertyFilesAutoConfiguation {
}
