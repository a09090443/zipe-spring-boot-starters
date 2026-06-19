package com.zipe.annotation;

import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 統一回應結果包裝的自訂 Annotation。
 *
 * <p>標記於 Controller 類別或方法上，搭配 {@link com.zipe.advice.ResponseResultBodyAdvice}
 * 使用，可自動將回傳值包裝成統一的 {@link com.zipe.dto.Result} 格式後序列化為 JSON 回應。</p>
 *
 * <p>由於組合了 {@link ResponseBody}，套用此 Annotation 後無需再另外標記
 * {@code @ResponseBody}。</p>
 *
 * <p>使用範例：</p>
 * <pre>{@code
 * @ResponseResultBody
 * @GetMapping("/hello")
 * public String hello() {
 *     return "world";
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@ResponseBody
public @interface ResponseResultBody {

    /**
     * 回應訊息的預設文字。
     *
     * <p>當未發生例外時，此文字會填入 {@link com.zipe.dto.Result} 的 {@code message} 欄位。</p>
     *
     * @return 回應訊息，預設為 {@code "OK"}
     */
    String message() default "OK";
}
