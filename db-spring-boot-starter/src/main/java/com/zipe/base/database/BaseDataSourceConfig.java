package com.zipe.base.database;

import com.zaxxer.hikari.HikariConfig;
import com.zipe.base.config.DataSourcePropertyConfig;
import com.zipe.base.database.datasourceconfig.HikariParamsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public abstract class BaseDataSourceConfig {
    protected Environment env;

    protected DataSourcePropertyConfig dynamicDataSource;

    @Autowired
    protected BaseDataSourceConfig(Environment env, DataSourcePropertyConfig dynamicDataSource) {
        this.env = env;
        this.dynamicDataSource = dynamicDataSource;
    }

    protected HikariConfig baseHikariConfig(HikariParamsConfig hikariConfig) {
        return hikariConfig.config();
    }

}
