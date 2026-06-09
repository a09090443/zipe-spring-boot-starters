package com.zipe.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zipe.annotation.ResponseResultBody;
import com.zipe.dto.Result;
import com.zipe.exception.IResultStatus;
import com.zipe.exception.ResultException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.util.WebUtils;

import java.util.Objects;

/**
 * 統一回應包裝與全域例外處理的 Advice。
 * <p>
 * 實作 {@link ResponseBodyAdvice}，攔截標記了 {@link ResponseResultBody} 的
 * Controller 類別或方法，將回傳值自動包裝為統一的 {@link com.zipe.dto.Result} 格式。
 * 同時透過 {@link ExceptionHandler} 接管全域例外，將錯誤轉換為一致的 JSON 回應，
 * 避免各 Controller 重複撰寫例外處理邏輯。
 * </p>
 */
@Slf4j
@RestControllerAdvice
// @Order(Ordered.HIGHEST_PRECEDENCE)
public class ResponseResultBodyAdvice implements ResponseBodyAdvice<Object> {
    /** Jackson ObjectMapper，用於將物件序列化為 JSON 字串（String 回傳型別需手動序列化）。 */
    @Resource
    private ObjectMapper objectMapper;

    /** 標記目標 Annotation 的 Class 物件，用於判斷類別或方法是否套用了統一回應包裝。 */
    private static final Class<ResponseResultBody> ANNOTATION_TYPE = ResponseResultBody.class;

    // @Autowired
    // private ObjectMapper objectMapper;

    /**
     * 判斷當前 Controller 類別或方法是否標記了 {@link ResponseResultBody}，
     * 只有符合條件時才會觸發 {@link #beforeBodyWrite} 進行回應包裝。
     *
     * @param returnType    方法的回傳型別資訊
     * @param converterType 實際使用的 {@link HttpMessageConverter} 型別
     * @return 若類別或方法任一存在 {@link ResponseResultBody} 則回傳 {@code true}
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ANNOTATION_TYPE) || returnType.hasMethodAnnotation(ANNOTATION_TYPE);
    }

    /**
     * 在回應主體寫出前攔截，將原始回傳值包裝為統一的 {@link Result} 格式。
     * <p>
     * 特別處理 {@link String} 回傳型別：Spring 使用 {@code StringHttpMessageConverter}
     * 處理字串，無法直接回傳物件，需手動序列化為 JSON 字串後回傳。
     * </p>
     *
     * @param body                  原始回傳物件
     * @param returnType            方法的回傳型別資訊
     * @param selectedContentType   選定的 Content-Type
     * @param selectedConverterType 選定的 {@link HttpMessageConverter} 型別
     * @param request               當前 HTTP 請求
     * @param response              當前 HTTP 回應
     * @return 包裝後的回應物件；若原始回傳型別為 {@link String} 則回傳 JSON 字串
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        ResponseResultBody mergedAnnotation = AnnotatedElementUtils.getMergedAnnotation(returnType.getContainingClass(), ANNOTATION_TYPE);

        Class<?> returnClass = returnType.getMethod().getReturnType();
        if (body instanceof String || Objects.equals(returnClass, String.class)) {
            // String 回傳型別由 StringHttpMessageConverter 處理，不支援物件轉換，
            // 必須先手動序列化為 JSON 字串，確保回應格式一致。
            String value = null;
            try {
                value = objectMapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return value;
        }
        return convert(body, mergedAnnotation);
    }

    /**
     * 將原始回傳物件轉換為統一的 {@link Result} 格式。
     *
     * <p>轉換邏輯如下：
     * <ol>
     *   <li>若 {@code body} 已是 {@link Result} 型別，直接透傳，避免雙重包裝。</li>
     *   <li>若 {@link ResponseResultBody#message()} 為預設值 {@code "OK"}，
     *       呼叫 {@link Result#success(Object)}。</li>
     *   <li>否則以自訂 message 建立匿名 {@link IResultStatus} 並呼叫
     *       {@link Result#success(IResultStatus, Object)}。</li>
     * </ol>
     * </p>
     *
     * @param body              原始回傳物件
     * @param mergedAnnotation  從 Controller 類別或方法取得的 {@link ResponseResultBody} 合併注解
     * @return 包裝後的 {@link Result} 物件
     */
    private Result<?> convert(Object body, ResponseResultBody mergedAnnotation) {

        if (body instanceof Result) {
            return (Result<?>) body;
        }
        if ("OK".equals(mergedAnnotation.message())) {
            return Result.success(body);
        }
        return Result.success(new IResultStatus() {
            @Override
            public HttpStatus getHttpStatus() {
                return HttpStatus.OK;
            }

            @Override
            public Integer getCode() {
                return 200;
            }

            @Override
            public String getMessage() {
                return mergedAnnotation.message();
            }
        }, body);

    }

    /**
     * 提供對標準Spring MVC異常的處理
     *
     * @param ex the target exception
     * @param request the current request
     */
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Result<?>> exceptionHandler(Exception ex, WebRequest request) {
        log.error("ExceptionHandler: {}", ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof ResultException) {
            return this.handleResultException((ResultException) ex, headers, request);
        }
        return this.handleException(ex, headers, request);
    }

    /**
     * 處理 {@link ResultException} 並組裝對應的 HTTP 回應。
     *
     * <p>從例外中取出 {@link com.zipe.enums.ResultStatus}，
     * 呼叫 {@link Result#failure(IResultStatus)} 建立錯誤回應，
     * HTTP 狀態碼依 {@code resultStatus.getHttpStatus()} 決定。</p>
     *
     * @param ex      攜帶 {@link com.zipe.enums.ResultStatus} 的業務例外
     * @param headers 回應標頭（可供子類別擴充）
     * @param request 當前 Web 請求
     * @return 對應錯誤狀態的 {@link ResponseEntity}
     */
    protected ResponseEntity<Result<?>> handleResultException(ResultException ex, HttpHeaders headers, WebRequest request) {
        Result<?> body = Result.failure(ex.getResultStatus());
        HttpStatus status = ex.getResultStatus().getHttpStatus();
        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * 處理所有未知例外（非 {@link ResultException}），統一回傳 HTTP 500 錯誤回應。
     *
     * <p>會以 {@code log.error} 記錄完整的例外堆疊追蹤，
     * 並回傳預設的 {@link Result#failure()}（{@code code=500, message="Internal Server Error"}）。</p>
     *
     * @param ex      任意未知例外
     * @param headers 回應標頭（可供子類別擴充）
     * @param request 當前 Web 請求
     * @return HTTP 500 的 {@link ResponseEntity}
     */
    protected ResponseEntity<Result<?>> handleException(Exception ex, HttpHeaders headers, WebRequest request) {
        Result<?> body = Result.failure();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unknown error.:{}", ex);
        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * 最終組裝例外回應的共用方法，參考
     * {@code ResponseEntityExceptionHandler#handleExceptionInternal} 的設計。
     *
     * <p>統一在此建立 {@link ResponseEntity}。若 HTTP 狀態為 500，
     * 會將例外物件寫入 request attribute（{@link WebUtils#ERROR_EXCEPTION_ATTRIBUTE}），
     * 供 Spring 框架後續的錯誤頁面渲染使用。</p>
     *
     * @param ex      已攔截的例外
     * @param body    要寫入回應的 {@link Result} 物件
     * @param headers 回應標頭
     * @param status  HTTP 狀態碼
     * @param request 當前 Web 請求
     * @return 組裝完成的 {@link ResponseEntity}
     */
    protected ResponseEntity<Result<?>> handleExceptionInternal(
            Exception ex, Result<?> body, HttpHeaders headers, HttpStatus status, WebRequest request) {

        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }
        return new ResponseEntity<>(body, headers, status);
    }
}