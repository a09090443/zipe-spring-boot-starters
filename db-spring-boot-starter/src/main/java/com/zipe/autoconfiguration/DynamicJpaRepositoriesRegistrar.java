package com.zipe.autoconfiguration;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.util.Streamable;
import org.springframework.util.StringUtils;

/**
 * 以設定檔驅動的 JPA Repository 註冊器。
 *
 * <p>讀取 {@code dynamic.base-packages} 屬性（逗號分隔可指定多個套件），並於對應套件下
 * 掃描、註冊 Spring Data JPA Repository，等同於在啟動類別宣告
 * {@code @EnableJpaRepositories(basePackages = ...)}，差別在於套件清單來自設定檔而非寫死於原始碼。</p>
 *
 * <h2>為何需要此機制</h2>
 * <p>db-starter 自建 {@code entityManagerFactory} 會使 Spring Boot 的 JPA 自動配置退讓；
 * 而引入如 iam-starter 等自帶 {@code @EnableJpaRepositories} 的模組後，依 Spring Boot 規則，
 * <b>容器中只要出現任何顯式 {@code @EnableJpaRepositories}，框架對應用主套件的 Repository 自動掃描即停止</b>。
 * 結果是應用自身的 Repository（如 {@code com.example.repository}）不再被掃描，啟動時報
 * {@code No qualifying bean of type ...Repository}。</p>
 *
 * <p>過去的解法是要求應用在啟動類別自行加上 {@code @EnableJpaRepositories(basePackages = "...")}；
 * 改由本註冊器後，應用只需在 {@code data-source.properties} 設定 {@code dynamic.base-packages}，
 * 無須改動原始碼，亦與 iam 的 {@code com.zipe.repository} 各自獨立掃描、互不影響。</p>
 *
 * <h2>行為</h2>
 * <ul>
 *   <li>未設定 {@code dynamic.base-packages} 時不做任何事，維持 Spring Boot 既有行為（向後相容）。</li>
 *   <li>有設定時，以 {@link AnnotationRepositoryConfigurationSource} 搭配
 *       {@link RepositoryConfigurationDelegate} 程式化註冊各套件下的 Repository。</li>
 * </ul>
 *
 * @author : Gary Tsai
 */
public class DynamicJpaRepositoriesRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    /** 驅動 Repository 掃描的設定屬性鍵；值為逗號分隔的套件清單。 */
    static final String BASE_PACKAGES_PROPERTY = "dynamic.base-packages";

    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 依 {@code dynamic.base-packages} 設定，於指定套件註冊 JPA Repository。
     *
     * @param importingClassMetadata 觸發匯入的設定類別中繼資料（此處未使用其屬性）
     * @param registry               Bean 定義註冊表
     * @param importBeanNameGenerator 匯入用的 Bean 命名產生器（轉交 Spring Data 沿用）
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
                                        BeanNameGenerator importBeanNameGenerator) {
        String[] basePackages = resolveBasePackages();
        if (basePackages.length == 0) {
            // 未設定 dynamic.base-packages：維持 Spring Boot 預設掃描行為，不介入
            return;
        }

        // 以一個僅帶預設值的 @EnableJpaRepositories 提供註解屬性骨架，再覆寫 base packages
        AnnotationMetadata metadata = AnnotationMetadata.introspect(EnableJpaRepositoriesDefaults.class);
        AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
                metadata, EnableJpaRepositories.class, resourceLoader, environment, registry, importBeanNameGenerator) {
            @Override
            public Streamable<String> getBasePackages() {
                // 註解屬性不解析 ${} placeholder、不切逗號，故以設定檔解析後的套件清單取代之
                return Streamable.of(basePackages);
            }
        };

        new RepositoryConfigurationDelegate(configurationSource, resourceLoader, environment)
                .registerRepositoriesIn(registry, new JpaRepositoryConfigExtension());
    }

    /**
     * 自 {@link Environment} 解析 {@code dynamic.base-packages}，逗號分隔並去除空白。
     *
     * <p>於 Environment 直接讀取（而非依賴已繫結的 {@code DataSourcePropertyConfig} Bean），
     * 是因本註冊器在設定類別解析階段即執行，當下 {@code @ConfigurationProperties} Bean 尚未就緒。</p>
     *
     * @return 去除空白後的套件陣列；未設定時回傳空陣列
     */
    private String[] resolveBasePackages() {
        String raw = Binder.get(environment).bind(BASE_PACKAGES_PROPERTY, String.class).orElse(null);
        if (!StringUtils.hasText(raw)) {
            return new String[0];
        }
        return StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(raw));
    }

    /** 僅用於提供 {@code @EnableJpaRepositories} 預設註解屬性的骨架類別，本身不掃描任何套件。 */
    @EnableJpaRepositories
    private static final class EnableJpaRepositoriesDefaults {
    }
}
