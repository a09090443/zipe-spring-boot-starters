package com.zipe.autoconfiguration;

import org.apache.catalina.Context;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;

/**
 * Tomcat 嵌入式伺服器的自動配置類別。
 * <p>
 * 在 {@link ServletWebServerConfiguration} 之前套用，
 * 以便自訂 Tomcat 的行為（例如關閉 JAR Manifest 掃描，加速應用程式啟動）。
 * 僅在 {@link WebAutoConfiguration} 存在於 Classpath 時才會生效。
 * </p>
 *
 * @author Gary Tsai
 */
@AutoConfiguration
@ConditionalOnClass({WebAutoConfiguration.class})
@AutoConfigureBefore(ServletWebServerConfiguration.class)
public class TomcatAutoConfiguration {

    /**
     * 建立並回傳自訂的 {@link TomcatServletWebServerFactory} Bean。
     * <p>
     * 該 Factory 會在 Tomcat Context 初始化後停用 JAR Manifest 掃描，
     * 避免啟動時耗費時間掃描不必要的 Manifest 條目。
     * </p>
     *
     * @return 已套用自訂後處理邏輯的 {@link TomcatServletWebServerFactory} 實例
     */
    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        return new CustomTomcatServletWebServerFactory();
    }

    /**
     * 客製化的 {@link TomcatServletWebServerFactory}，
     * 覆寫 {@code postProcessContext} 以調整 Tomcat Context 的掃描行為。
     */
    static class CustomTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

        /**
         * 在 Tomcat Context 建立後進行後處理。
         * <p>
         * 將 {@link StandardJarScanner} 的 {@code scanManifest} 設為 {@code false}，
         * 避免掃描 JAR 的 {@code MANIFEST.MF} 中的 Class-Path 條目，
         * 有效縮短應用程式的啟動時間。
         * </p>
         *
         * @param context 目前正在初始化的 Tomcat {@link Context}
         */
        @Override
        protected void postProcessContext(Context context) {
            // 停用 JAR Manifest 掃描，避免啟動時耗費時間掃描不必要的 Class-Path 條目
            ((StandardJarScanner) context.getJarScanner()).setScanManifest(false);
        }
    }
}
