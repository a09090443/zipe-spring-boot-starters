package com.zipe.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zipe.enums.ResultStatus;
import org.junit.jupiter.api.Test;

/**
 * 驗證 {@link Result} 各靜態工廠方法的狀態碼、訊息與 data 包裝，含 null 狀態的 fallback 行為。
 */
class ResultTest {

    /**
     * success() 應為成功狀態（code=200、message=OK）且 data 為 null。
     */
    @Test
    void successWithoutData() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("OK", result.getMessage());
        assertNull(result.getData());
    }

    /**
     * success(data) 應攜帶資料本體。
     */
    @Test
    void successWithData() {
        Result<String> result = Result.success("payload");
        assertEquals(200, result.getCode());
        assertEquals("payload", result.getData());
    }

    /**
     * success(null, data) 在狀態為 null 時應 fallback 至 success(data)。
     */
    @Test
    void successWithNullStatusFallsBack() {
        Result<String> result = Result.success(null, "payload");
        assertEquals(200, result.getCode());
        assertEquals("payload", result.getData());
    }

    /**
     * success(status, data) 應使用指定狀態。
     */
    @Test
    void successWithCustomStatus() {
        Result<String> result = Result.success(ResultStatus.BAD_REQUEST, "payload");
        assertEquals(400, result.getCode());
        assertEquals("payload", result.getData());
    }

    /**
     * failure() 應為內部錯誤狀態（code=500）。
     */
    @Test
    void failureWithoutStatus() {
        Result<Void> result = Result.failure();
        assertEquals(500, result.getCode());
        assertEquals("Internal Server Error", result.getMessage());
    }

    /**
     * failure(null) 在狀態為 null 時應 fallback 至 500。
     */
    @Test
    void failureWithNullStatusFallsBack() {
        Result<Void> result = Result.failure(null);
        assertEquals(500, result.getCode());
        assertNull(result.getData());
    }

    /**
     * failure(status, data) 應使用指定狀態並攜帶資料。
     */
    @Test
    void failureWithCustomStatusAndData() {
        Result<String> result = Result.failure(ResultStatus.BAD_REQUEST, "detail");
        assertEquals(400, result.getCode());
        assertEquals("detail", result.getData());
    }
}
