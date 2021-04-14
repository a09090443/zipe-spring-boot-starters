package com.zipe.base.database;

import com.zaxxer.hikari.HikariConfig;
import com.zipe.base.config.DataSourcePropertyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

public abstract class BaseDataSourceConfig {
    protected Environment env;

    protected DataSourcePropertyConfig dynamicDataSource;

    @Autowired
    public BaseDataSourceConfig(Environment env, DataSourcePropertyConfig dynamicDataSource) {
        this.env = env;
        this.dynamicDataSource = dynamicDataSource;
    }

    @Bean
    protected HikariConfig baseHikariConfig() {
        HikariConfig config = new HikariConfig();
        //是否自定義配置，為true時下面兩個引數才生效
        config.addDataSourceProperty("cachePrepStmts", "true");
        //連線池大小預設25，官方推薦250-500
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        //單條語句最大長度預設256，官方推薦2048
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        //新版本MySQL支援伺服器端準備，開啟能夠得到顯著效能提升
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("useLocalTransactionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setIdleTimeout(30000);
        config.setPoolName("Hikari");
        config.setMaxLifetime(2000000);
        config.setConnectionTimeout(30000);
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

}
