package com.zipe.keycloak.support;

import com.google.auto.service.AutoService;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.common.util.ResteasyProvider;

/**
 * RESTEasy 3 的 {@link ResteasyProvider} 實作，供 Keycloak 嵌入式伺服器使用。
 *
 * <p>透過 {@link AutoService} 自動註冊為 SPI 服務，讓 Keycloak 在啟動時能夠
 * 透過 Java SPI 機制找到並使用此提供者，無需手動設定 META-INF/services 檔案。</p>
 *
 * <p>此類別將 Keycloak 的 {@link ResteasyProvider} 介面委派至
 * {@link ResteasyProviderFactory} 的靜態方法，負責管理 RESTEasy 執行緒本地
 * 的 Context 物件生命週期。</p>
 */
@AutoService(ResteasyProvider.class)
public class Resteasy3Provider implements ResteasyProvider {

    /**
     * 從 RESTEasy 的執行緒本地 Context 中取得指定型別的物件。
     *
     * @param <R>  回傳型別的泛型參數
     * @param type 欲取得的 Context 物件型別
     * @return 對應型別的 Context 物件；若不存在則回傳 {@code null}
     */
    @Override
    public <R> R getContextData(Class<R> type) {
        return ResteasyProviderFactory.getContextData(type);
    }

    /**
     * 將物件設定為 RESTEasy {@link Dispatcher} 的預設 Context 物件。
     *
     * <p>預設 Context 物件會在整個應用程式生命週期內有效，不會隨每次請求重置，
     * 適合用於需要全域共享的服務或設定物件。</p>
     *
     * @param type     物件對應的型別 Class，作為 Map 的鍵值
     * @param instance 要設定的物件實例
     */
    @Override
    public void pushDefaultContextObject(Class type, Object instance) {
        // 取得 Dispatcher 後，將物件放入其預設 Context 物件的 Map 中
        getContextData(Dispatcher.class).getDefaultContextObjects().put(type, instance);
    }

    /**
     * 將物件推入 RESTEasy 執行緒本地的 Context 堆疊。
     *
     * <p>推入的物件僅在當前執行緒的請求處理過程中有效，請求結束後應呼叫
     * {@link #clearContextData()} 或由 RESTEasy 框架自動清除。</p>
     *
     * @param type     物件對應的型別 Class
     * @param instance 要推入的物件實例
     */
    @Override
    public void pushContext(Class type, Object instance) {
        ResteasyProviderFactory.pushContext(type, instance);
    }

    /**
     * 清除當前執行緒的所有 RESTEasy Context 資料。
     *
     * <p>通常在請求處理完成後呼叫，以防止執行緒池環境下的 Context 資料洩漏。</p>
     */
    @Override
    public void clearContextData() {
        ResteasyProviderFactory.clearContextData();
    }

}
