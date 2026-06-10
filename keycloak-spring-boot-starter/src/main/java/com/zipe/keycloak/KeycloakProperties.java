package com.zipe.keycloak;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Keycloak 設定屬性載體。
 *
 * <p>透過 {@code @ConfigurationProperties(prefix = "keycloak")} 將 {@code application.yml}
 * 中所有 {@code keycloak.*} 的鍵值對自動繫結至此 {@link HashMap}，
 * 供嵌入式 Keycloak 伺服器啟動時讀取動態屬性。
 *
 * <p>繼承 {@link HashMap} 而非宣告具體欄位，是為了支援 Keycloak 大量且結構多變的設定鍵，
 * 避免每次新增屬性都需異動此類別。Lombok 的 {@code @Getter}/{@code @Setter}
 * 提供 Map 本身以外的標準存取方法。
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties extends HashMap<String, Object> {

    /**
     * 將繫結後因索引式屬性鍵（{@code foo[0]}、{@code foo[1]}…）而被收斂成
     * {@link java.util.LinkedHashMap} 的 YAML 序列，正規化還原為 {@link java.util.List}。
     *
     * <p>由於本類別為無型別的 {@code Map<String, Object>}，Spring Binder 無法將 YAML 清單
     * 推斷為集合，會以「數字字串為鍵」的 Map 形式繫結。此方法於繫結完成後（{@code @PostConstruct}
     * 早於 Binder 之後執行）遞迴走訪整個結構，把這類索引式 Map 轉回 {@code List}，
     * 讓下游（如 {@code Config.Scope#getArray}）能以一致的集合型別讀取。</p>
     */
    @PostConstruct
    public void normalizeIndexedLists() {
        normalize(this);
    }

    @SuppressWarnings("unchecked")
    private static void normalize(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> child) {
                Map<String, Object> childMap = (Map<String, Object>) child;
                if (isIndexKeyed(childMap)) {
                    entry.setValue(new ArrayList<>(childMap.values()));
                } else {
                    // 非索引式 Map，繼續往下層遞迴處理巢狀設定
                    normalize(childMap);
                }
            }
        }
    }

    /**
     * 判斷 Map 的鍵是否為 {@code "0"、"1"、"2"…} 的連續索引，藉此辨識出被攤平的 YAML 序列。
     *
     * @param map 待判斷的 Map
     * @return 當且僅當所有鍵依序為連續數字字串時回傳 {@code true}
     */
    private static boolean isIndexKeyed(Map<String, Object> map) {
        if (map.isEmpty()) {
            return false;
        }
        int i = 0;
        for (String key : map.keySet()) {
            if (!String.valueOf(i++).equals(key)) {
                return false;
            }
        }
        return true;
    }
}
