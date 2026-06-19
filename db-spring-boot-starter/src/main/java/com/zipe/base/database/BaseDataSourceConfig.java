package com.zipe.base.database;

import com.zaxxer.hikari.HikariConfig;
import com.zipe.base.config.DataSourcePropertyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 資料來源設定的抽象基底類別。
 *
 * <p>提供 HikariCP 連線池的共用預設設定，子類別可繼承後依需求覆寫或擴充，
 * 實現單一或多個資料來源的具體組裝邏輯。</p>
 *
 * <p>本類別持有 {@link Environment} 與 {@link DataSourcePropertyConfig}，
 * 供子類別存取應用程式屬性與多資料來源設定。</p>
 */
public abstract class BaseDataSourceConfig {
    /** Spring 環境物件，供子類別讀取應用程式屬性。 */
    protected Environment env;

    /** 多資料來源屬性設定，封裝 {@code application.yml} 中的資料來源清單。 */
    protected DataSourcePropertyConfig dynamicDataSource;

    /**
     * 透過建構子注入共用相依物件。
     *
     * @param env           Spring {@link Environment}，用於取得設定屬性
     * @param dynamicDataSource 多資料來源屬性設定物件
     */
    @Autowired
    protected BaseDataSourceConfig(Environment env, DataSourcePropertyConfig dynamicDataSource) {
        this.env = env;
        this.dynamicDataSource = dynamicDataSource;
    }

    /**
     * 建立並回傳套用效能最佳化參數的 HikariCP 基礎設定 Bean。
     *
     * <p>依據 HikariCP 官方建議與 MySQL Connector/J 最佳實務，預先啟用
     * PreparedStatement 快取、伺服器端預編譯、批次改寫等選項，以提升高並行
     * 環境下的資料庫存取效能。子類別可取得此 Bean 後進一步設定 JDBC URL、
     * 帳號密碼等資料來源特定屬性。</p>
     *
     * @return 已套用共用效能設定的 {@link HikariConfig} 實例
     */
    @Bean
    protected HikariConfig baseHikariConfig() {
        HikariConfig config = new HikariConfig();
        // 是否啟用自訂 PreparedStatement 快取設定，為 true 時下面兩個參數才生效
        config.addDataSourceProperty("cachePrepStmts", "true");
        // 連線池 PreparedStatement 快取大小預設 25，官方推薦 250-500
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        // 單條語句最大快取長度預設 256，官方推薦 2048
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        // 新版本 MySQL 支援伺服器端預編譯，啟用後可獲得顯著效能提升
        config.addDataSourceProperty("useServerPrepStmts", "true");
        // 使用本地端 Session 狀態快取，減少與伺服器的往返次數
        config.addDataSourceProperty("useLocalSessionState", "true");
        // 使用本地端交易狀態追蹤，避免多餘的 rollback/commit 查詢
        config.addDataSourceProperty("useLocalTransactionState", "true");
        // 將多筆 INSERT/UPDATE 合併為批次語句，提升批次寫入效能
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        // 快取 ResultSet 的中繼資料，降低重複查詢的解析成本
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        // 快取伺服器端設定，減少每次連線的初始化查詢
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        // 省略冗餘的 SET autocommit 指令，降低網路往返
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        // 停用時間統計，減少不必要的計時開銷
        config.addDataSourceProperty("maintainTimeStats", "false");
        // 連線池最小閒置連線數
        config.setMinimumIdle(5);
        // 連線池最大連線數
        config.setMaximumPoolSize(20);
        // 閒置連線逾時時間（毫秒）
        config.setIdleTimeout(30000);
        config.setPoolName("Hikari");
        // 連線最大存活時間（毫秒），需小於資料庫的 wait_timeout 設定
        config.setMaxLifetime(2000000);
        // 取得連線的最大等待時間（毫秒）
        config.setConnectionTimeout(30000);
        config.setAutoCommit(true);
        // 連線存活驗證查詢，確保池中連線有效
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

}
