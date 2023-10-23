package com.zipe.base.database.datasourceconfig;

import com.zaxxer.hikari.HikariConfig;

public class AS400HikariConfig implements HikariParamsConfig{
    @Override
    public HikariConfig config() {
        HikariConfig config = new HikariConfig();
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setIdleTimeout(30000);
        config.setPoolName("Hikari");
        config.setMaxLifetime(2000000);
        config.setConnectionTimeout(30000);
        config.setAutoCommit(true);
        // AS400 語法不能使用 SELECT 1
        config.setConnectionTestQuery("VALUES 1");
        return config;
    }
}
