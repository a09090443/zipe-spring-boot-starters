package com.zipe.keycloak.scripting;

import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticatorSpi;
import org.keycloak.authentication.authenticators.browser.DeployedScriptAuthenticatorFactory;
import org.keycloak.authorization.policy.provider.PolicySpi;
import org.keycloak.authorization.policy.provider.js.DeployedScriptPolicyFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.ProtocolMapperSpi;
import org.keycloak.protocol.oidc.mappers.DeployedScriptOIDCProtocolMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.ProviderManagerDeployer;
import org.keycloak.provider.ProviderManagerRegistry;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.representations.provider.ScriptProviderMetadata;
import org.keycloak.util.JsonSerialization;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 嵌入式腳本元件自動掃描與註冊器。
 *
 * <p>負責在 Keycloak 啟動時，從 classpath 中的
 * {@code META-INF/keycloak-scripts.json} 讀取腳本元件描述檔，
 * 並將 Authenticator（認證器）、OIDC Protocol Mapper（協定映射器）
 * 以及 Authorization Policy（授權策略）等腳本型元件動態部署至 Keycloak。</p>
 *
 * <p>實作 {@link AuthenticatorFactory} 僅為讓 Keycloak SPI 機制能透過
 * {@link com.google.auto.service.AutoService} 自動發現此類別；
 * 實際的 {@link org.keycloak.authentication.Authenticator} 建立邏輯不在此類別中。</p>
 *
 * <p>同時實作 {@link EnvironmentDependentProviderFactory}，
 * 以確認 {@link org.keycloak.common.Profile.Feature#SCRIPTS} 功能已啟用。</p>
 */
@Slf4j
@AutoService(AuthenticatorFactory.class)
public class EmbeddedScriptBasedComponentRegistrar implements AuthenticatorFactory, EnvironmentDependentProviderFactory {

    /** classpath 中 Keycloak 腳本描述檔的位置 */
    public static final String KEYCLOAK_SCRIPTS_JSON_LOCATION = "META-INF/keycloak-scripts.json";

    /**
     * 回傳此 Provider 的唯一識別碼。
     *
     * @return provider 識別碼字串
     */
    @Override
    public String getId() {
        return "script-component-registrar";
    }

    /**
     * 此實作不建立實際的 {@link Authenticator}，固定回傳 {@code null}。
     *
     * @param session 當前的 Keycloak Session（未使用）
     * @return 永遠為 {@code null}
     */
    @Override
    public Authenticator create(KeycloakSession session) {
        // NOT USED
        return null;
    }

    /**
     * 初始化 Provider（此實作無需任何初始化動作）。
     *
     * @param config Keycloak 設定範疇（未使用）
     */
    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    /**
     * 在所有 Provider 初始化完成後呼叫，負責掃描並部署腳本元件。
     *
     * <p>先嘗試從 classpath 讀取腳本描述檔；若無描述檔則直接跳過，
     * 否則將描述檔中定義的所有腳本元件部署至 Keycloak。</p>
     *
     * @param factory 已完成初始化的 {@link KeycloakSessionFactory}，需實作 {@link ProviderManagerDeployer}
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {

        KeycloakScripts keycloakScripts = discoverScriptComponents();
        if (keycloakScripts == null) {
            return;
        }

        deployKeycloakScriptComponents((ProviderManagerDeployer) factory, keycloakScripts);
    }

    /**
     * 從 classpath 讀取 {@code META-INF/keycloak-scripts.json}，
     * 解析並封裝腳本元件描述資訊。
     *
     * @return 解析後的 {@link KeycloakScripts}；若描述檔不存在或讀取失敗則回傳 {@code null}
     */
    private KeycloakScripts discoverScriptComponents() {

        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(KEYCLOAK_SCRIPTS_JSON_LOCATION)) {

            if (in == null) {
                log.info("Could detect any script-based components.");
                return null;
            }

            KeycloakScripts keycloakScripts = new KeycloakScripts(JsonSerialization.readValue(in, ScriptProviderDescriptor.class));
            List<ScriptProviderMetadata> authenticators = keycloakScripts.getAuthenticators();
            List<ScriptProviderMetadata> oidcMappers = keycloakScripts.getOidcMappers();
            List<ScriptProviderMetadata> policies = keycloakScripts.getPolicies();
            log.info("Detected script-based components. authenticators={} mappers={} policies={}", authenticators.size(), oidcMappers.size(), policies.size());

            return keycloakScripts;
        } catch (IOException e) {
            log.info("Failed to load detect any script-based components on classpath from {}.", KEYCLOAK_SCRIPTS_JSON_LOCATION, e);
        }

        return null;
    }


    /**
     * 將腳本元件描述資訊部署至 Keycloak。
     *
     * <p>依序將 Authenticator、OIDC Protocol Mapper 及 Authorization Policy
     * 加入 {@link KeycloakDeploymentInfo}，再透過 {@link ProviderManagerRegistry}
     * 完成動態部署。</p>
     *
     * @param factory        用於觸發部署的 {@link ProviderManagerDeployer}
     * @param keycloakScripts 已解析完成的腳本元件清單
     */
    private void deployKeycloakScriptComponents(ProviderManagerDeployer factory, KeycloakScripts keycloakScripts) {

        KeycloakDeploymentInfo kdi = KeycloakDeploymentInfo.create().name("script-components");

        List<ScriptProviderMetadata> authenticators = keycloakScripts.getAuthenticators();
        authenticators.stream()
                .map(this::initScriptComponent)
                .forEach(sc -> kdi.addProvider(AuthenticatorSpi.class, new DeployedScriptAuthenticatorFactory(sc)));

        List<ScriptProviderMetadata> oidcMappers = keycloakScripts.getOidcMappers();
        oidcMappers.stream()
                .map(this::initScriptComponent)
                .forEach(sc -> kdi.addProvider(ProtocolMapperSpi.class, new DeployedScriptOIDCProtocolMapper(sc)));

        List<ScriptProviderMetadata> policies = keycloakScripts.getPolicies();
        policies.stream()
                .map(this::initScriptComponent)
                .forEach(sc -> kdi.addProvider(PolicySpi.class, new DeployedScriptPolicyFactory(sc)));

        ProviderManager pm = new ProviderManager(kdi, Thread.currentThread().getContextClassLoader());

        // Ugly hack to trigger script deployment...
        ProviderManagerRegistry.SINGLETON.setDeployer(factory);
        ProviderManagerRegistry.SINGLETON.deploy(pm);
    }

    /**
     * 初始化單一腳本元件的 metadata，包含設定 id、補齊 name 以及載入腳本內容。
     *
     * @param scriptMetadata 從描述檔解析出的腳本 metadata（會直接修改此物件並回傳）
     * @return 已完成初始化的 {@link ScriptProviderMetadata}
     */
    private ScriptProviderMetadata initScriptComponent(ScriptProviderMetadata scriptMetadata) {

        scriptMetadata.setId("script" + "-" + scriptMetadata.getFileName());

        String name = scriptMetadata.getName();
        if (name == null) {
            name = scriptMetadata.getFileName();
        }
        scriptMetadata.setName(name);

        String scriptLocation = "./scripts/" + scriptMetadata.getFileName();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptLocation)) {
            scriptMetadata.setCode(StreamUtils.copyToString(in, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.warn("Cannot load script from {}", scriptMetadata.getFileName());
        }

        return scriptMetadata;
    }

    /**
     * 釋放資源（此實作無需任何清理動作）。
     */
    @Override
    public void close() {
        // NOOP
    }

    /**
     * 判斷當前 Keycloak 環境是否支援此 Provider。
     *
     * <p>僅在 {@link org.keycloak.common.Profile.Feature#SCRIPTS} 功能已啟用時回傳 {@code true}。</p>
     *
     * @return 若 SCRIPTS 功能已啟用則為 {@code true}，否則為 {@code false}
     */
    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.SCRIPTS);
    }

    /**
     * 回傳顯示名稱（此 Provider 不需要在 UI 顯示，固定回傳 {@code null}）。
     *
     * @return 永遠為 {@code null}
     */
    @Override
    public String getDisplayType() {
        return null;
    }

    /**
     * 回傳參考分類（此 Provider 不屬於任何分類，固定回傳 {@code null}）。
     *
     * @return 永遠為 {@code null}
     */
    @Override
    public String getReferenceCategory() {
        return null;
    }

    /**
     * 此 Provider 不支援設定，固定回傳 {@code false}。
     *
     * @return 永遠為 {@code false}
     */
    @Override
    public boolean isConfigurable() {
        return false;
    }

    /**
     * 回傳可用的認證執行需求選項（此 Provider 不提供任何選項）。
     *
     * @return 空陣列
     */
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[0];
    }

    /**
     * 此 Provider 不允許使用者自行設定，固定回傳 {@code false}。
     *
     * @return 永遠為 {@code false}
     */
    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    /**
     * 回傳說明文字（此 Provider 不需要說明文字，固定回傳 {@code null}）。
     *
     * @return 永遠為 {@code null}
     */
    @Override
    public String getHelpText() {
        return null;
    }

    /**
     * 回傳可設定的屬性清單（此 Provider 無需任何設定屬性）。
     *
     * @return 空集合
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    /**
     * 封裝從 {@code keycloak-scripts.json} 解析出的腳本元件描述資訊。
     *
     * <p>提供分類存取各類腳本元件清單的便利方法，
     * 若描述檔中無對應分類則安全地回傳空集合，避免 {@code NullPointerException}。</p>
     */
    @RequiredArgsConstructor
    static class KeycloakScripts {

        /** 從 JSON 描述檔反序列化而來的腳本 Provider 描述物件 */
        private final ScriptProviderDescriptor descriptor;

        /**
         * 取得所有腳本型 Authenticator（認證器）的 metadata 清單。
         *
         * @return Authenticator metadata 清單；若無定義則回傳空集合
         */
        public List<ScriptProviderMetadata> getAuthenticators() {
            return Optional.ofNullable(descriptor.getProviders().get(ScriptProviderDescriptor.AUTHENTICATORS)).orElse(Collections.emptyList());
        }

        /**
         * 取得所有腳本型 OIDC Protocol Mapper（協定映射器）的 metadata 清單。
         *
         * @return OIDC Mapper metadata 清單；若無定義則回傳空集合
         */
        public List<ScriptProviderMetadata> getOidcMappers() {
            return Optional.ofNullable(descriptor.getProviders().get(ScriptProviderDescriptor.MAPPERS)).orElse(Collections.emptyList());
        }

        /**
         * 取得所有腳本型 Authorization Policy（授權策略）的 metadata 清單。
         *
         * @return Policy metadata 清單；若無定義則回傳空集合
         */
        public List<ScriptProviderMetadata> getPolicies() {
            return Optional.ofNullable(descriptor.getProviders().get(ScriptProviderDescriptor.POLICIES)).orElse(Collections.emptyList());
        }
    }
}
