package com.zipe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * iam-starter 的設定屬性，對應前綴 {@code iam.*}。
 * <p>
 * 控制 iam 模組整體的啟用與否、內建 REST API、群組授權前綴與建表行為。
 * 各群組以巢狀靜態類表達，對應 {@code iam.api.*}、{@code iam.group.*}、{@code iam.ddl.*}。
 * </p>
 *
 * @author Gary.Tsai
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "iam")
public class IamProperties {

    /** 總開關，關閉則不裝配任何 iam Bean。預設 {@code true}。 */
    private boolean enabled = true;

    /** 內建 REST API 相關設定。 */
    private Api api = new Api();

    /** 群組授權相關設定。 */
    private Group group = new Group();

    /** 建表（DDL）相關設定。 */
    private Ddl ddl = new Ddl();

    /**
     * 內建 REST Controller 設定（對應 {@code iam.api.*}）。
     */
    @Getter
    @Setter
    public static class Api {

        /** 是否啟用內建 REST Controller。預設 {@code true}。 */
        private boolean enabled = true;

        /** 內建 API 路由前綴。預設 {@code /api/iam}。 */
        private String basePath = "/api/iam";
    }

    /**
     * 群組授權設定（對應 {@code iam.group.*}）。
     */
    @Getter
    @Setter
    public static class Group {

        /** 群組 code 轉 authority 時所套用的前綴。預設 {@code ROLE_}。 */
        private String rolePrefix = "ROLE_";
    }

    /**
     * 建表（DDL）設定（對應 {@code iam.ddl.*}）。
     */
    @Getter
    @Setter
    public static class Ddl {

        /** 是否由 starter 提供的 {@code schema-iam.sql} 自動建表。預設 {@code false}。 */
        private boolean init = false;
    }
}
