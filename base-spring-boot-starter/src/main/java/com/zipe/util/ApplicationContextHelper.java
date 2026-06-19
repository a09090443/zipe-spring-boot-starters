package com.zipe.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring ApplicationContext 靜態存取輔助工具。
 *
 * <p>實作 {@link ApplicationContextAware}，於 Spring 容器啟動時自動注入
 * {@link ApplicationContext}，之後可透過靜態方法在非 Spring 管理的物件中
 * 取得容器內的 Bean，適用於工廠類別、工具類別等無法透過 {@code @Autowired}
 * 注入的情境。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/4/23 下午 13:44
 **/
public class ApplicationContextHelper implements ApplicationContextAware {

    /** 由 Spring 容器在啟動時注入，供後續靜態方法存取。 */
    private static ApplicationContext applicationContext;

    /**
     * Spring 容器初始化時回呼此方法，將 {@link ApplicationContext} 保存至靜態欄位，
     * 以便後續透過靜態方法取得 Bean。
     *
     * @param applicationContext Spring 應用程式上下文
     * @throws BeansException 若設定過程發生錯誤時拋出
     */
    @Override
    @SuppressWarnings("static-access")
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 依 Bean 名稱從 Spring 容器中取得 Bean 實例。
     *
     * <p>若 {@link ApplicationContext} 尚未初始化（例如在容器啟動前呼叫），
     * 將拋出 {@link NullPointerException}。</p>
     *
     * @param beanName 在 Spring 容器中註冊的 Bean 名稱
     * @return 對應名稱的 Bean 實例
     * @throws NullPointerException 若 ApplicationContext 尚未初始化
     */
    public static Object getBean(String beanName) {
        if (null == applicationContext) {
            throw new NullPointerException("ApplicationContext is null!");
        }
        return applicationContext.getBean(beanName);
    }

    /**
     * 依類別型別從 Spring 容器中取得 Bean 實例。
     *
     * <p>與 {@link #getBean(String)} 不同，此方法在 {@link ApplicationContext}
     * 尚未初始化時會回傳 {@code null} 而非拋出例外，適合非強制依賴的情境。</p>
     *
     * @param <T>   Bean 的型別
     * @param clazz 目標 Bean 的 Class 物件
     * @return 對應型別的 Bean 實例；若 ApplicationContext 尚未初始化則回傳 {@code null}
     */
    public static <T> T popBean(Class<T> clazz) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 依 Bean 名稱與類別型別從 Spring 容器中取得 Bean 實例。
     *
     * <p>同時指定名稱與型別可避免容器中有多個相同型別 Bean 時產生歧義，
     * 並確保回傳值已完成型別轉換。</p>
     *
     * @param <T>   Bean 的型別
     * @param name  在 Spring 容器中註冊的 Bean 名稱
     * @param clazz 目標 Bean 的 Class 物件
     * @return 對應名稱與型別的 Bean 實例；若 ApplicationContext 尚未初始化則回傳 {@code null}
     */
    public static <T> T popBean(String name, Class<T> clazz) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getBean(name, clazz);
    }
}
