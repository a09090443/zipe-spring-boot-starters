package com.zipe.base.database.datasourceconfig;

import com.zaxxer.hikari.HikariConfig;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;

import javax.sql.DataSource;

public class JndiHikariConfig implements HikariParamsConfig {
    private final String jndiName;

    public JndiHikariConfig(String jndiName) {
        this.jndiName = jndiName;
    }

    @Override
    public HikariConfig config() {
        final JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
        dataSourceLookup.setResourceRef(true);
        DataSource dataSourceTemp = null;
        try {
            dataSourceTemp = dataSourceLookup.getDataSource(jndiName);
        } catch (DataSourceLookupFailureException e) {
            throw new DataSourceLookupFailureException("Jndi:" + jndiName + " 連線失敗");
        }
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(dataSourceTemp);
        return hikariConfig;
    }
}
