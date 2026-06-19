package com.zipe.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zipe.testsupport.repository.DummyRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 驗證 {@link DynamicJpaRepositoriesRegistrar} 依 {@code dynamic.base-packages} 設定，
 * 程式化掃描並註冊指定套件下的 Spring Data JPA Repository。
 *
 * <p>這是「引入自帶 {@code @EnableJpaRepositories} 的模組（如 iam-starter）後，
 * 應用改以設定檔重新啟用自身 Repository 掃描、無須在啟動類別補宣告」此一行為的回歸保護。</p>
 */
class DynamicJpaRepositoriesRegistrarTest {

    /** 以指定的 {@code dynamic.base-packages} 屬性建立並重新整理應用上下文。 */
    private AnnotationConfigApplicationContext context(String basePackages) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        if (basePackages != null) {
            ConfigurableEnvironment env = ctx.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource(
                    "test", Map.of(DynamicJpaRepositoriesRegistrar.BASE_PACKAGES_PROPERTY, basePackages)));
        }
        ctx.register(TestJpaConfig.class);
        ctx.refresh();
        return ctx;
    }

    @Test
    void registersRepositoriesFromBasePackagesProperty() {
        try (AnnotationConfigApplicationContext ctx = context("com.zipe.testsupport.repository")) {
            assertNotNull(ctx.getBean(DummyRepository.class),
                    "dynamic.base-packages 指定的套件下，其 Repository 應被掃描並註冊為 Bean");
        }
    }

    @Test
    void doesNothingWhenPropertyAbsent() {
        try (AnnotationConfigApplicationContext ctx = context(null)) {
            assertEquals(0, ctx.getBeanNamesForType(DummyRepository.class).length,
                    "未設定 dynamic.base-packages 時不應主動註冊任何 Repository");
        }
    }

    /** 提供 H2 資料來源、EntityManagerFactory、交易管理器，並匯入受測的註冊器。 */
    @Configuration
    @Import(DynamicJpaRepositoriesRegistrar.class)
    static class TestJpaConfig {

        @Bean
        DataSource dataSource() {
            return new SimpleDriverDataSource(new org.h2.Driver(),
                    "jdbc:h2:mem:dynamicRepo;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean(name = "entityManagerFactory")
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.zipe.testsupport.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            Properties properties = new Properties();
            properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop");
            factory.setJpaProperties(properties);
            return factory;
        }

        @Bean(name = "transactionManager")
        PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
