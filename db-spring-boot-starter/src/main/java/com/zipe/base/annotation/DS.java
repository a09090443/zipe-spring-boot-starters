package com.zipe.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自訂資料來源（Data Source）切換注解。
 *
 * <p>標記於方法或類別上，搭配 {@code DynamicDataSourceAspect} AOP 切面，
 * 在方法執行前將執行緒綁定的資料來源切換至指定名稱，
 * 方法結束後自動還原，以實現多資料來源的動態切換。</p>
 *
 * <p>使用範例：</p>
 * <pre>{@code
 * // 切換至名稱為 "secondary" 的資料來源
 * @DS("secondary")
 * public List<User> findAllFromSecondary() { ... }
 * }</pre>
 *
 * @see com.zipe.base.aspect.DynamicDataSourceAspect
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DS {
    /**
     * 指定要切換的資料來源名稱，須與設定檔中定義的資料來源鍵值一致。
     * 若未指定，預設使用名稱為 {@code "common"} 的資料來源。
     *
     * @return 資料來源名稱
     */
    String value() default "common";
}
