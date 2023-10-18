package com.zipe.dto;

import com.zipe.enums.ResultStatus;
import com.zipe.exception.IResultStatus;
import lombok.Getter;
import lombok.ToString;

/**
 * 後端回傳給前端的json格式
 * 返回格式可參考 org.springframework.boot.web.servlet.error.DefaultErrorAttributes
 */
@Getter
@ToString
public class Result<T> {
    /**
     * 錯誤代碼
     */
    private Integer code;
    /**
     * 訊息說明
     */
    private String message;
    /**
     * 返回內容
     */
    private T data;

    private Result(IResultStatus resultStatus, T data) {
        this.code = resultStatus.getCode();
        this.message = resultStatus.getMessage();
        this.data = data;
    }

    /**
     * 返回成功
     */
    public static Result<Void> success() {
        return new Result<Void>(ResultStatus.SUCCESS, null);
    }

    /**
     * 返回成功並回傳資料
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(ResultStatus.SUCCESS, data);
    }

    /**
     * 返回成功並回傳資料並自訂狀態
     */
    public static <T> Result<T> success(IResultStatus resultStatus, T data) {
        if (resultStatus == null) {
            return success(data);
        }
        return new Result<T>(resultStatus, data);
    }

    /**
     * 錯誤返回
     */
    public static <T> Result<T> failure() {
        return new Result<T>(ResultStatus.INTERNAL_SERVER_ERROR, null);
    }

    /**
     * 返回錯誤並回傳自訂狀態
     */
    public static <T> Result<T> failure(IResultStatus resultStatus) {
        return failure(resultStatus, null);
    }

    /**
     * 返回錯誤並回傳資料並自訂狀態
     */
    public static <T> Result<T> failure(IResultStatus resultStatus, T data) {
        if (resultStatus == null) {
            return new Result<T>(ResultStatus.INTERNAL_SERVER_ERROR, null);
        }
        return new Result<T>(resultStatus, data);
    }
}