package com.zipe.base.config;

import com.zipe.base.model.DynamicDataSourceConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author : Gary Tsai
 * @created : @Date 2021/3/12 下午 02:01
 **/
@Configuration
@ConfigurationProperties(prefix = "dynamic")
@Data
public class DataSourcePropertyConfig {

    private String primary;
    private String entityScan;
    private Map<String, DynamicDataSourceConfig> dataSourceMap;

    @Bean("dynamicDataSource")
    public DataSourcePropertyConfig dynamicDataSource(){
        DataSourcePropertyConfig dataSourcePropertyConfig = new DataSourcePropertyConfig();
        dataSourcePropertyConfig.setPrimary(primary);
        dataSourcePropertyConfig.setEntityScan(entityScan);
        dataSourcePropertyConfig.setDataSourceMap(dataSourceMap);
        return dataSourcePropertyConfig;
    }
}
