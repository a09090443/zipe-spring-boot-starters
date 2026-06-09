package com.zipe.keycloak.support;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.platform.PlatformProvider;
import org.keycloak.services.ServicesLogger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SmartApplicationListener;

import java.io.File;

/**
 * Spring Boot 環境下的 Keycloak 平台提供者（PlatformProvider）。
 *
 * <p>透過 {@link AutoService} 自動註冊為 {@link PlatformProvider} SPI 實作，
 * 同時實作 {@link SmartApplicationListener} 以監聽 Spring 應用程式事件，
 * 在應用程式就緒時啟動 Keycloak，在 Context 停止時執行關閉流程。</p>
 */
@Slf4j
@AutoService(PlatformProvider.class)
public class SpringBootPlatformProvider implements PlatformProvider, SmartApplicationListener {

    /** Keycloak 執行期間使用的暫存目錄，於啟動時建立。 */
    protected File tmpDir;

    /** 應用程式啟動完成後由 Keycloak 框架注入的啟動回呼。 */
    protected Runnable onStartup;

    /** 應用程式停止時由 Keycloak 框架注入的關閉回呼。 */
    protected Runnable onShutdown;

    /**
     * 處理 Spring 應用程式事件。
     *
     * <p>收到 {@link ApplicationReadyEvent} 時觸發 Keycloak 啟動流程；
     * 收到 {@link ContextStoppedEvent} 時觸發關閉流程。</p>
     *
     * @param event Spring 應用程式事件
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        if (event instanceof ApplicationReadyEvent) {
            startup();
        } else if (event instanceof ContextStoppedEvent) {
            shutdown();
        }
    }

    /**
     * 宣告此監聽器僅關心 {@link ApplicationReadyEvent} 與 {@link ContextStoppedEvent} 兩種事件類型，
     * 避免接收到不必要的其他 Spring 事件。
     *
     * @param eventType 待判斷的事件類型
     * @return 若為受支援的事件類型則回傳 {@code true}，否則回傳 {@code false}
     */
    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationReadyEvent.class.equals(eventType) || ContextStoppedEvent.class.equals(eventType);
    }

    /**
     * 回傳此監聽器的唯一識別 ID，以類別全名作為識別字串，確保不與其他監聽器衝突。
     *
     * @return 此監聽器的唯一識別 ID
     */
    @Override
    public String getListenerId() {
        return this.getClass().getName();
    }

    /**
     * 由 Keycloak 框架呼叫，注入啟動回呼。
     * 當應用程式就緒後，此回呼將在 {@link #startup()} 中被執行。
     *
     * @param onStartup Keycloak 框架提供的啟動回呼
     */
    @Override
    public void onStartup(@SuppressWarnings("hiding") Runnable onStartup) {
        this.onStartup = onStartup;
    }

    /**
     * 由 Keycloak 框架呼叫，注入關閉回呼。
     * 當應用程式停止時，此回呼將在 {@link #shutdown()} 中被執行。
     *
     * @param onShutdown Keycloak 框架提供的關閉回呼
     */
    @Override
    public void onShutdown(@SuppressWarnings("hiding") Runnable onShutdown) {
        this.onShutdown = onShutdown;
    }

    /**
     * 強制終止 Keycloak 服務，並將原始例外包裝為 {@link RuntimeException} 向上拋出。
     *
     * <p>此方法在 Keycloak 發生無法復原的嚴重錯誤時被呼叫。</p>
     *
     * @param cause 導致強制退出的原始例外
     * @throws RuntimeException 包裝後的執行期例外，確保呼叫端可感知到錯誤
     */
    @Override
    public void exit(Throwable cause) {
        log.error("exit", cause);
        ServicesLogger.LOGGER.fatal(cause);
        throw new RuntimeException(cause);
    }

    /**
     * 回傳 Keycloak 執行期間使用的暫存目錄。
     *
     * @return 暫存目錄的 {@link File} 物件
     */
    @Override
    public File getTmpDirectory() {
        return tmpDir;
    }

    /**
     * 執行 Keycloak 關閉流程，呼叫框架注入的關閉回呼。
     */
    protected void shutdown() {
        this.onShutdown.run();
    }

    /**
     * 執行 Keycloak 啟動流程：先建立暫存目錄，再呼叫框架注入的啟動回呼。
     */
    protected void startup() {
        this.tmpDir = createTempDir();
        this.onStartup.run();
    }

    /**
     * 建立 Keycloak 所需的暫存目錄。
     *
     * <p>以系統預設暫存路徑（{@code java.io.tmpdir}）為基底，
     * 建立名為 {@code keycloak-spring-tmp} 的子目錄。
     * 若目錄已存在則直接使用，無需重複建立。</p>
     *
     * @return 已建立或已存在的暫存目錄 {@link File} 物件
     * @throws RuntimeException 若無法建立且目錄不存在時拋出
     */
    protected File createTempDir() {
        String tmpDirBase = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirBase, "keycloak-spring-tmp");
        boolean couldCreateDirs = tmpDir.mkdirs();
        // 目錄成功建立或已存在時均視為可用
        if (couldCreateDirs || tmpDir.exists()) {
            log.debug("Using server tmp directory: {}", tmpDir.getAbsolutePath());
            return tmpDir;
        }

        throw new RuntimeException("Failed to create temp directory: " + tmpDir.getAbsolutePath());
    }
}
