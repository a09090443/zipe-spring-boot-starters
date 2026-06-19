package com.zipe.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zipe.base.config.DataSourcePropertyConfig;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * 驗證 db-starter 自建的 EntityManagerFactory 在組裝掃描套件時，
 * 會同時納入 {@code dynamic.entity-scan} 與容器中以 {@code @EntityScan} 註冊的套件。
 *
 * <p>這是「引入 iam-starter 等以 {@code @EntityScan} 宣告 Entity 的 starter 時，
 * 無須在 {@code dynamic.entity-scan} 重複設定其套件」此一行為的回歸保護。</p>
 */
class EntityScanMergeTest {

    private static final String DUMMY = "com.zipe.testsupport.entity.DummyEntity";

    /** 以指定的 entity-scan 設定建立受測的自動配置實例。 */
    private DataSourceConfigAutoConfiguration config(String entityScan) {
        DataSourcePropertyConfig cfg = new DataSourcePropertyConfig();
        cfg.setEntityScan(entityScan);
        HibernateProperties hibernateProperties = new HibernateProperties();
        hibernateProperties.setDdlAuto("create-drop");
        return new DataSourceConfigAutoConfiguration(new StandardEnvironment(), cfg, hibernateProperties);
    }

    /** 建立獨立的 H2 記憶體資料來源。 */
    private DataSource h2(String name) {
        return new SimpleDriverDataSource(new org.h2.Driver(),
                "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    /** 取出 EntityManagerFactory 實際管理的實體類別全名集合。 */
    private Set<String> managedTypes(LocalContainerEntityManagerFactoryBean emf) {
        return emf.getObject().getMetamodel().getEntities().stream()
                .map(e -> e.getJavaType().getName())
                .collect(Collectors.toSet());
    }

    @Test
    void mergesAtEntityScanRegisteredPackages() {
        // 不設定 dynamic.entity-scan，純靠容器中以 @EntityScan 註冊的套件
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        EntityScanPackages.register(beanFactory, "com.zipe.testsupport.entity");

        LocalContainerEntityManagerFactoryBean emf =
                config(null).multiEntityManager(h2("emfMerge"), beanFactory);
        try {
            assertTrue(managedTypes(emf).contains(DUMMY),
                    "以 @EntityScan 註冊的套件其 Entity 應被 EntityManagerFactory 管理");
        } finally {
            emf.destroy();
        }
    }

    @Test
    void supportsCommaSeparatedEntityScan() {
        // 容器中無 @EntityScan，改以逗號分隔的 dynamic.entity-scan 指定多套件
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        LocalContainerEntityManagerFactoryBean emf =
                config("com.zipe.nonexistent,com.zipe.testsupport.entity")
                        .multiEntityManager(h2("emfComma"), beanFactory);
        try {
            assertTrue(managedTypes(emf).contains(DUMMY),
                    "逗號分隔的 dynamic.entity-scan 應逐一掃描各套件");
        } finally {
            emf.destroy();
        }
    }
}
