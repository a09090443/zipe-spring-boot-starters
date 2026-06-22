package com.zipe.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.zipe.enums.ResultStatus;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link ResultException} 兩個建構子的預設狀態與訊息設定。
 */
class ResultExceptionTest {

    /**
     * 無參數建構子應使用 INTERNAL_SERVER_ERROR，並以其 message 作為例外訊息。
     */
    @Test
    void defaultConstructorUsesInternalServerError() {
        ResultException exception = new ResultException();
        assertEquals(ResultStatus.INTERNAL_SERVER_ERROR, exception.getResultStatus());
        assertEquals("Internal Server Error", exception.getMessage());
    }

    /**
     * 指定狀態的建構子應保留該狀態，並以其 message 作為例外訊息。
     */
    @Test
    void statusConstructorUsesGivenStatus() {
        ResultException exception = new ResultException(ResultStatus.BAD_REQUEST);
        assertEquals(ResultStatus.BAD_REQUEST, exception.getResultStatus());
        assertEquals("Bad Request", exception.getMessage());
    }

    /**
     * ResultException 應為 Exception 的子型別（受檢例外）。
     */
    @Test
    void isCheckedException() {
        assertInstanceOf(Exception.class, new ResultException());
    }
}
