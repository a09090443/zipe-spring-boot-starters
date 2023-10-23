package com.zipe.base.database.datasourceconfig;

import com.zaxxer.hikari.HikariConfig;

public class DB2HikariConfig implements HikariParamsConfig{
    @Override
    public HikariConfig config() {
        HikariConfig config = new HikariConfig();
        //是否自定義配置，為true時下面兩個引數才生效
        config.addDataSourceProperty("cachePrepStmts", "true");
        //連線池大小預設25，官方推薦250-500
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        //單條語句最大長度預設256，官方推薦2048
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setIdleTimeout(30000);
        config.setPoolName("Hikari");
        config.setMaxLifetime(2000000);
        config.setConnectionTimeout(30000);
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT current date FROM sysibm.sysdummy1");
        return config;
    }
}
