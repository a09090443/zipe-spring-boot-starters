package com.zipe.util;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * 支援 YAML 格式的 {@link PropertySourceFactory} 實作。
 * <p>
 * Spring 預設的 {@code @PropertySource} 僅支援 {@code .properties} 檔案格式；
 * 透過此工廠類別，可在 {@code @PropertySource} 中指定 {@code factory} 屬性，
 * 使其同時支援 {@code .yaml} / {@code .yml} 與 {@code .properties} 三種格式。
 * 當副檔名無法辨識時，則退回使用 {@link DefaultPropertySourceFactory} 處理。
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/04/24 下午 22:41
 **/
public class YamlPropertySourceFactory implements PropertySourceFactory {

    /**
     * 依據資源檔案的副檔名，建立對應的 {@link PropertySource}。
     * <ul>
     *   <li>{@code .yaml} / {@code .yml}：使用 {@link YamlPropertiesFactoryBean} 解析後包裝為 {@link PropertiesPropertySource}。</li>
     *   <li>{@code .properties}：委派給 {@link DefaultPropertySourceFactory} 處理。</li>
     *   <li>無法辨識的格式或檔名為空：同樣委派給 {@link DefaultPropertySourceFactory}。</li>
     * </ul>
     *
     * @param name     屬性來源的名稱；若無法從檔名取得，則使用此名稱作為備援
     * @param resource 已編碼的資源包裝物件，內含實際的設定檔資源
     * @return 對應的 {@link PropertySource} 實例
     * @throws IOException 讀取資源失敗時拋出
     */
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {

        // e.g. system.yaml, system.properties
        String resourceFileName = resource.getResource().getFilename();

        // 無法取得檔名時，退回使用預設工廠以避免 NullPointerException
        if (resourceFileName == null || resourceFileName.length() == 0) {
            return new DefaultPropertySourceFactory().createPropertySource(name, resource);
        }

        if (resourceFileName.endsWith("yaml") || resourceFileName.endsWith("yml")) {
            // 使用 Spring 內建的 YAML 解析工廠，將 YAML 結構展開為扁平化的 Properties
            YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
            factoryBean.setResources(resource.getResource());
            Properties properties = factoryBean.getObject();

            // 以檔名作為 PropertySource 的唯一識別名稱，避免多檔案載入時發生名稱衝突
            return new PropertiesPropertySource(resourceFileName, properties);
        }

        if (resourceFileName.endsWith(".properties")) {
            // .properties 檔直接交由預設工廠處理，保持原生行為
            return new DefaultPropertySourceFactory().createPropertySource(resourceFileName, resource);
        }

        // 副檔名不符合已知格式時，以傳入的 name 參數作為來源名稱進行兜底處理
        return new DefaultPropertySourceFactory().createPropertySource(name, resource);
    }
}
