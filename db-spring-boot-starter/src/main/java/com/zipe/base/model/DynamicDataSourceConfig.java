package com.zipe.base.model;

import lombok.Data;

/**
 * 動態資料來源的連線設定模型。
 *
 * <p>用於承載從外部設定檔（例如 {@code application.yml}）讀取的單一資料來源連線資訊，
 * 包含名稱識別、JDBC URL、帳號密碼及驅動程式類別名稱。
 * 通常由 {@code DataSourcePropertyConfig} 解析並組成清單，
 * 再透過 {@code BaseDataSourceConfig} 建立實際的 {@code DataSource} Bean。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/3/12 下午 02:12
 **/
@Data
public class DynamicDataSourceConfig {
    /** 資料來源的識別名稱，對應 {@code @DS} 註解中指定的名稱。 */
    private String name;

    /** JDBC 連線 URL，例如 {@code jdbc:mysql://localhost:3306/mydb}。 */
    private String url;

    /** 資料庫連線帳號。 */
    private String username;

    /** 資料庫連線密碼。 */
    private String pa55word;

    /** JDBC 驅動程式的完整類別名稱，例如 {@code com.mysql.cj.jdbc.Driver}。 */
    private String driverClassName;
}
