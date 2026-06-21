package com.zipe.exception;

import com.zipe.enums.ResultStatus;
import lombok.Getter;

/**
 * 業務層受檢例外，攜帶 {@link ResultStatus} 以描述錯誤狀態。
 *
 * <p>拋出此例外後，{@link com.zipe.advice.ResponseResultBodyAdvice} 的全域例外處理器
 * 會自動攔截，並依 {@code resultStatus} 中的 HTTP 狀態碼與業務代碼，
 * 組裝成統一的 {@link com.zipe.dto.Result} JSON 回應。</p>
 *
 * <p><b>注意：</b>建構子目前接受的是具體枚舉 {@link ResultStatus}，
 * 而非 {@link com.zipe.exception.IResultStatus} 介面，
 * 若需傳入自訂狀態碼，請直接使用 {@code Result.failure(customStatus)} 或繼承本類別擴充。</p>
 */
@Getter
public class ResultException extends Exception {

    /**
     * 異常資訊訊息
     */
    ResultStatus resultStatus;

    /**
     * 以預設狀態（{@link ResultStatus#INTERNAL_SERVER_ERROR}，HTTP 500）建立例外。
     */
    public ResultException() {
        this(ResultStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 以指定的 {@link ResultStatus} 建立例外，並將其 {@code message} 設為例外訊息。
     *
     * @param resultStatus 描述錯誤的狀態枚舉；不得為 {@code null}
     */
    public ResultException(ResultStatus resultStatus) {
        super(resultStatus.getMessage());
        this.resultStatus = resultStatus;
    }
}
