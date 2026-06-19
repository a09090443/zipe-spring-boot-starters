package com.zipe.exception;

import org.springframework.http.HttpStatus;

/**
 * 回應狀態策略介面，定義統一回應結構所需的狀態碼、業務代碼與說明訊息。
 *
 * <p>業務系統可實作此介面，定義專屬的錯誤碼枚舉，搭配 {@link com.zipe.dto.Result}
 * 的靜態工廠方法（如 {@code Result.failure(IResultStatus)}）使用，
 * 不需修改 Starter 內建的 {@link com.zipe.enums.ResultStatus}。</p>
 *
 * <p>使用範例：</p>
 * <pre>{@code
 * public enum AppStatus implements IResultStatus {
 *     USER_NOT_FOUND(HttpStatus.NOT_FOUND, 1001, "User not found");
 *     // ...
 * }
 * }</pre>
 */
public interface IResultStatus {

    /**
     * 取得對應的 HTTP 狀態碼，用於設定 HTTP 回應的狀態行。
     *
     * @return Spring {@link HttpStatus} 枚舉值
     */
    HttpStatus getHttpStatus();

    /**
     * 取得業務層錯誤代碼，對應回應 JSON 中的 {@code code} 欄位。
     *
     * @return 業務錯誤代碼（例如 200、400、500 或自訂業務碼）
     */
    Integer getCode();

    /**
     * 取得狀態說明訊息，對應回應 JSON 中的 {@code message} 欄位。
     *
     * @return 說明文字（例如 {@code "OK"}、{@code "User not found"}）
     */
    String getMessage();
}
