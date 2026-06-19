package com.zipe.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Spring Security Web 應用程式初始化器。
 *
 * <p>繼承 {@link AbstractSecurityWebApplicationInitializer}，
 * 負責在 Servlet 容器啟動時自動將 {@code springSecurityFilterChain} 註冊為最優先的
 * {@code DelegatingFilterProxy}，確保所有請求在進入應用程式之前都經過 Spring Security 過濾鏈處理。</p>
 *
 * <p>此類別採用無參數建構子形式，表示 Spring Security 的根應用程式上下文
 * 由外部（例如 {@code ContextLoaderListener}）負責載入；
 * 若需要在同一類別中同時載入 Spring MVC 與 Security 上下文，
 * 則應改用帶有 {@code Class<?>...} 參數的建構子。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2020/11/23 下午 01:24
 **/
public class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {
}
