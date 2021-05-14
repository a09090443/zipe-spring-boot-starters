package com.zipe.autoconfiguration;

import org.apache.catalina.Context;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Gary Tsai
 */
@Configuration
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
public class TomcatAutoConfiguration {
    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        return new CustomTomcatServletWebServerFactory();
    }

    static class CustomTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

        @Override
        protected void postProcessContext(Context context) {
            ((StandardJarScanner) context.getJarScanner()).setScanManifest(false);
        }
    }
}
