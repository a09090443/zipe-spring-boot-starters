package com.zipe.base.config;

import com.zipe.base.model.DynamicDataSourceConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 動態多資料來源屬性設定類別。
 *
 * <p>透過 {@code @ConfigurationProperties(prefix = "dynamic")} 將 {@code application.yml}
 * 中 {@code dynamic.*} 前綴的屬性自動繫結至此類別，供後續動態資料來源切換機制使用。</p>
 *
 * <p>設定範例：
 * <pre>
 * dynamic:
 *   primary: master
 *   entityScan: com.example.entity
 *   isEncrypt: false
 *   dataSourceMap:
 *     master:
 *       url: jdbc:mysql://localhost:3306/db
 *       username: root
 *       password: secret
 * </pre>
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/3/12 下午 02:01
 **/
@Configuration
@ConfigurationProperties(prefix = "dynamic")
@Data
public class DataSourcePropertyConfig {

    /** 預設（主要）資料來源的名稱，對應 {@code dataSourceMap} 中的 key。 */
    private String primary;

    /**
     * JPA Entity 掃描的基礎套件路徑，用於多資料來源時限定各資料來源管轄的實體範圍。
     * <p>可以逗號分隔指定多個套件（例如 {@code com.example,com.zipe.entity}），
     * 以便同時管理跨模組的 Entity（如併用 iam-starter 時）。</p>
     */
    private String entityScan;

    /** 是否對資料來源的連線密碼進行加解密處理，預設為 {@code false}（不加密）。 */
    private Boolean isEncrypt = false;

    /** 資料來源設定的對應表，key 為資料來源名稱，value 為對應的連線設定。 */
    private Map<String, DynamicDataSourceConfig> dataSourceMap;

    /**
     * 將目前繫結完成的屬性值包裝為一個新的 {@link DataSourcePropertyConfig} Bean，
     * 並以 {@code "dynamicDataSource"} 為名稱註冊至 Spring 容器，
     * 供其他元件（如 {@code BaseDataSourceConfig}）依名稱注入使用。
     *
     * <p>此方法手動複製各屬性值，是為了確保回傳的 Bean 物件與目前設定物件解耦，
     * 避免因 Spring 代理機制導致屬性值意外被覆蓋。</p>
     *
     * @return 已填入所有動態資料來源屬性的 {@link DataSourcePropertyConfig} 實例
     */
    @Bean("dynamicDataSource")
    public DataSourcePropertyConfig dynamicDataSource(){
        // 建立新實例並逐一複製屬性，確保 Bean 與當前設定物件相互獨立
        DataSourcePropertyConfig dataSourcePropertyConfig = new DataSourcePropertyConfig();
        dataSourcePropertyConfig.setPrimary(primary);
        dataSourcePropertyConfig.setEntityScan(entityScan);
        dataSourcePropertyConfig.setIsEncrypt(isEncrypt);
        dataSourcePropertyConfig.setDataSourceMap(dataSourceMap);
        return dataSourcePropertyConfig;
    }
}
