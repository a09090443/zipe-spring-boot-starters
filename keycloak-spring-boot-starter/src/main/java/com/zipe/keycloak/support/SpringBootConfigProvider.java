package com.zipe.keycloak.support;

import com.zipe.keycloak.KeycloakProperties;
import lombok.RequiredArgsConstructor;
import org.keycloak.Config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 將 Keycloak 的設定鍵值查詢委派至 Spring Boot Properties 的設定提供者。
 *
 * <p>實作 {@link Config.ConfigProvider} 介面，使 Keycloak 內部的設定讀取
 * 能夠直接從 {@link KeycloakProperties}（即 {@code application.yml} 中的
 * {@code keycloak.*} 屬性）取得對應值，無需另外維護獨立的 Keycloak 設定檔。</p>
 *
 * <p>本類別採用單例模式（Singleton），在建構時將自身實例存入靜態欄位，
 * 以便 Keycloak SPI 機制在任何時機皆可透過 {@link #getInstance()} 取得同一實例。</p>
 */
public class SpringBootConfigProvider implements Config.ConfigProvider {

    /** 全域單例實例，由建構子在初始化時設定，供 Keycloak SPI 框架靜態存取。 */
    private static SpringBootConfigProvider INSTANCE;

    /** 來自 Spring Boot 的 Keycloak 屬性物件，作為所有設定值的資料來源。 */
    private final KeycloakProperties keycloakProperties;

    /**
     * 建構 {@code SpringBootConfigProvider} 並將自身註冊為全域單例。
     *
     * <p>由於 Keycloak SPI 框架需要靜態方式取得提供者，建構子執行時
     * 會同步將 {@code this} 指派給 {@link #INSTANCE}。</p>
     *
     * @param keycloakProperties Spring Boot 整合的 Keycloak 屬性物件
     */
    public SpringBootConfigProvider(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
        INSTANCE = this;
    }

    /**
     * 取得已建立的全域單例實例。
     *
     * @return {@code SpringBootConfigProvider} 的單例，若尚未建構則為 {@code null}
     */
    public static SpringBootConfigProvider getInstance() {
        return INSTANCE;
    }

    /**
     * 取得指定 SPI 的預設提供者名稱。
     *
     * <p>對應 Keycloak 設定中 {@code <spi>.provider} 的值。</p>
     *
     * @param spi SPI 名稱
     * @return 提供者名稱字串，若未設定則為 {@code null}
     */
    @Override
    public String getProvider(String spi) {
        return scope(spi).get("provider");
    }

    /**
     * 根據給定的範圍路徑（scope path）從 {@link KeycloakProperties} 中取得對應的設定範圍。
     *
     * <p>方法會依序以每個 scope 名稱逐層深入 {@code KeycloakProperties} 的巢狀 Map 結構，
     * 最終回傳對應層級的 {@link MapScope}。若路徑為空或對應值不存在，則回傳空範圍。</p>
     *
     * @param scopes 範圍路徑，可為多層巢狀，例如 {@code "storage", "file"}
     * @return 對應範圍的 {@link Config.Scope} 實作
     */
    @Override
    public Config.Scope scope(String... scopes) {

        if (scopes == null || scopes.length == 0) {
            return new MapScope(keycloakProperties);
        }

        String scope = scopes[0];
        Object currentScopeValue = keycloakProperties.get(scope);
        // 若對應的值不是 Map，表示此路徑不存在任何子設定，回傳空範圍
        if (!(currentScopeValue instanceof Map)) {
            return new MapScope(Collections.emptyMap());
        }

        Map<String, Object> scopeMap = (Map<String, Object>) currentScopeValue;
        // 從第二層起逐層深入 Map，以支援多層巢狀設定結構
        for (int i = 1; i < scopes.length; i++) {
            currentScopeValue = scopeMap.get(scopes[i]);
            if (currentScopeValue instanceof Map) {
                scopeMap = (Map<String, Object>) currentScopeValue;
            }
        }

        return new MapScope(scopeMap);
    }

    /**
     * 以 {@link Map} 為底層資料來源的 {@link Config.Scope} 實作。
     *
     * <p>封裝一個 {@code Map<String, Object>}，提供 Keycloak 所需的各型別設定值讀取方法，
     * 包含字串、整數、長整數、布林值及字串陣列。所有型別轉換均透過 {@link String#valueOf} 中介，
     * 以相容 YAML 解析後可能產生的多種物件型別。</p>
     */
    @RequiredArgsConstructor
    class MapScope implements Config.Scope {

        /** 儲存該範圍所有設定鍵值對的 Map。 */
        private final Map<String, Object> map;

        /**
         * 從 Map 中取得指定鍵的原始物件值。
         *
         * @param key          設定鍵名
         * @param defaultValue 當鍵不存在時回傳的預設值
         * @return 對應的物件值，或預設值
         */
        private Object getObject(String key, Object defaultValue) {
            Object obj = map.get(key);
            if (obj == null) {
                return defaultValue;
            }
            return obj;
        }

        /**
         * 取得指定鍵的字串值，不存在時回傳 {@code null}。
         *
         * @param key 設定鍵名
         * @return 對應的字串值，或 {@code null}
         */
        @Override
        public String get(String key) {
            return get(key, null);
        }

        /**
         * 取得指定鍵的字串值，不存在時回傳預設值。
         *
         * @param key          設定鍵名
         * @param defaultValue 不存在時的預設字串
         * @return 對應的字串值，或預設值
         */
        @Override
        public String get(String key, String defaultValue) {
            Object obj = getObject(key, defaultValue);
            if (obj == null) {
                return defaultValue;
            }
            return String.valueOf(obj);
        }

        /**
         * 取得指定鍵的字串陣列值。
         *
         * <p>Spring Boot 解析 YAML 清單時，可能將其轉換為 {@link LinkedHashMap}（以索引為鍵）
         * 而非 {@code String[]}，因此此處針對兩種情況分別處理。</p>
         *
         * @param key 設定鍵名
         * @return 字串陣列，若不存在則回傳空陣列
         */
        @Override
        public String[] getArray(String key) {
            Object obj = getObject(key, null);
            if (obj == null) {
                return new String[0];
            }

            // TODO find better way to parse config yaml into a list instead of a LinkedHashMap in the first place
            // YAML 序列被解析成 LinkedHashMap（以數字索引為鍵）時，逐一取出值轉為字串陣列
            if (obj instanceof LinkedHashMap) {
                LinkedHashMap lm = (LinkedHashMap) obj;
                String[] strings = new String[lm.size()];
                int i = 0;
                for (Object entryValue : lm.values()) {
                    strings[i++] = String.valueOf(entryValue);
                }
                return strings;
            }

            return (String[]) obj;
        }

        /**
         * 取得指定鍵的整數值，不存在時回傳 {@code null}。
         *
         * @param key 設定鍵名
         * @return 對應的整數值，或 {@code null}
         */
        @Override
        public Integer getInt(String key) {
            return getInt(key, null);
        }

        /**
         * 取得指定鍵的整數值，不存在時回傳預設值。
         *
         * @param key          設定鍵名
         * @param defaultValue 不存在時的預設整數
         * @return 對應的整數值，或預設值
         */
        @Override
        public Integer getInt(String key, Integer defaultValue) {
            Object obj = getObject(key, null);
            if (obj == null) {
                return defaultValue;
            }
            return Integer.valueOf(String.valueOf(obj));
        }

        /**
         * 取得指定鍵的長整數值，不存在時回傳 {@code null}。
         *
         * @param key 設定鍵名
         * @return 對應的長整數值，或 {@code null}
         */
        @Override
        public Long getLong(String key) {
            return getLong(key, null);
        }

        /**
         * 取得指定鍵的長整數值，不存在時回傳預設值。
         *
         * @param key          設定鍵名
         * @param defaultValue 不存在時的預設長整數
         * @return 對應的長整數值，或預設值
         */
        @Override
        public Long getLong(String key, Long defaultValue) {
            Object obj = getObject(key, null);
            if (obj == null) {
                return defaultValue;
            }
            return Long.valueOf(String.valueOf(obj));
        }

        /**
         * 取得指定鍵的布林值，不存在時回傳 {@code null}。
         *
         * @param key 設定鍵名
         * @return 對應的布林值，或 {@code null}
         */
        @Override
        public Boolean getBoolean(String key) {
            return getBoolean(key, null);
        }

        /**
         * 取得指定鍵的布林值，不存在時回傳預設值。
         *
         * @param key          設定鍵名
         * @param defaultValue 不存在時的預設布林值
         * @return 對應的布林值，或預設值
         */
        @Override
        public Boolean getBoolean(String key, Boolean defaultValue) {
            Object obj = getObject(key, null);
            if (obj == null) {
                return defaultValue;
            }
            return Boolean.valueOf(String.valueOf(obj));
        }

        /**
         * 委派至外層 {@link SpringBootConfigProvider#scope(String...)} 取得子範圍。
         *
         * @param path 子範圍路徑
         * @return 對應的 {@link Config.Scope}
         */
        @Override
        public Config.Scope scope(String... path) {
            return SpringBootConfigProvider.this.scope(path);
        }
    }
}
