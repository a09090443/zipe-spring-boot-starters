package com.zipe.keycloak;

import com.zipe.keycloak.support.SpringBootConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 嵌入式 Keycloak 應用程式進入點。
 *
 * <p>繼承 {@link KeycloakApplication}，在 Spring Boot 環境中啟動嵌入式 Keycloak 伺服器。
 * 負責在 bootstrap 階段完成以下初始化工作：
 * <ul>
 *   <li>透過 {@link SpringBootConfigProvider} 載入 Spring Boot 應用設定</li>
 *   <li>依設定決定是否建立 master realm 管理員帳號</li>
 *   <li>依設定決定是否匯入 realm 設定檔</li>
 * </ul>
 */
@Slf4j
public class EmbeddedKeycloakApplication extends KeycloakApplication {

    /** 自訂 Keycloak 屬性，包含管理員帳號設定與 realm 匯入設定。 */
    private final KeycloakCustomProperties customProperties;

    /**
     * 建構子。從 {@link ServletContext} 取得 Spring 應用程式上下文，
     * 並注入 {@link KeycloakCustomProperties}。
     *
     * @param context 由 JAX-RS 容器注入的 {@link ServletContext}
     */
    public EmbeddedKeycloakApplication(@Context ServletContext context) {
        this.customProperties = WebApplicationContextUtils.getRequiredWebApplicationContext(context).getBean(KeycloakCustomProperties.class);
    }

    /**
     * 覆寫 Keycloak 啟動引導方法，在父類別完成基本初始化後，
     * 依序執行管理員帳號建立與 realm 設定匯入。
     *
     * @return 完成初始化的 {@link ExportImportManager} 實例
     */
    @Override
    protected ExportImportManager bootstrap() {
        ExportImportManager exportImportManager = super.bootstrap();

        tryCreateMasterRealmAdminUser();
        tryImportRealm();

        return exportImportManager;
    }

    /**
     * 載入 Keycloak 設定，使用 {@link SpringBootConfigProvider} 將 Spring Boot
     * 的 application.yml / application.properties 橋接至 Keycloak 設定機制。
     */
    protected void loadConfig() {
        Config.init(SpringBootConfigProvider.getInstance());
    }

    /**
     * 嘗試在 master realm 建立管理員帳號。
     *
     * <p>若設定中停用了管理員帳號建立功能，或使用者名稱為空，則直接略過。
     * 密碼若未指定，將自動產生一組隨機 UUID 密碼，並輸出至日誌供後續查閱。
     * 若帳號已存在，則記錄警告並回滾交易。
     */
    protected void tryCreateMasterRealmAdminUser() {

        if (!customProperties.getAdminUser().isCreateAdminUserEnabled()) {
            log.warn("Skipping creation of keycloak master adminUser.");
            return;
        }

        KeycloakCustomProperties.AdminUser adminUser = customProperties.getAdminUser();

        String username = adminUser.getUsername();
        // 使用者名稱不可為空或空白，否則無法建立帳號
        if (!(StringUtils.hasLength(username) || StringUtils.hasText(username))) {
            return;
        }

        KeycloakSession session = getSessionFactory().create();
        KeycloakTransactionManager transaction = session.getTransactionManager();
        try {
            transaction.begin();

            boolean randomPassword = false;
            String password = adminUser.getPassword();
            // 若未設定密碼，自動產生隨機密碼，確保帳號可建立
            if (StringUtils.isEmpty(adminUser.getPassword())) {
                password = UUID.randomUUID().toString();
                randomPassword = true;
            }
            new ApplianceBootstrap(session).createMasterRealmUser(username, password);
            if (randomPassword) {
                log.info("Generated admin password: {}", password);
            }
            ServicesLogger.LOGGER.addUserSuccess(username, Config.getAdminRealm());

            transaction.commit();
        } catch (IllegalStateException e) {
            // 帳號已存在時 Keycloak 拋出 IllegalStateException，回滾並記錄
            transaction.rollback();
            ServicesLogger.LOGGER.addUserFailedUserExists(username, Config.getAdminRealm());
        } catch (Throwable t) {
            // 其他未預期錯誤，確保交易回滾，避免資料不一致
            transaction.rollback();
            ServicesLogger.LOGGER.addUserFailed(t, username, Config.getAdminRealm());
        } finally {
            session.close();
        }
    }

    /**
     * 嘗試從設定的匯入位置匯入 Keycloak realm 設定檔。
     *
     * <p>若指定的匯入檔案不存在，或無法讀取為 {@link File}，則記錄訊息後略過。
     * 匯入行為由 {@link ExportImportConfig} 控制，並透過 {@link ExportImportManager}
     * 執行實際的匯入作業。
     */
    protected void tryImportRealm() {

        KeycloakCustomProperties.Migration imex = customProperties.getMigration();
        Resource importLocation = imex.getImportLocation();

        if (!importLocation.exists()) {
            log.info("Could not find keycloak import file {}", importLocation);
            return;
        }

        File file;
        try {
            file = importLocation.getFile();
        } catch (IOException e) {
            log.error("Could not read keycloak import file {}", importLocation, e);
            return;
        }

        log.info("Starting Keycloak realm configuration import from location: {}", importLocation);

        KeycloakSession session = getSessionFactory().create();

        // 設定匯入動作、提供者及檔案路徑，交由 ExportImportManager 執行匯入
        ExportImportConfig.setAction("import");
        ExportImportConfig.setProvider(imex.getImportProvider());
        ExportImportConfig.setFile(file.getAbsolutePath());

        ExportImportManager manager = new ExportImportManager(session);
        manager.runImport();

        session.close();

        log.info("Keycloak realm configuration import finished.");
    }
}
