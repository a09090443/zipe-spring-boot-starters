package com.zipe.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 固化 {@link ResultStatus} 各常數所攜帶的 HttpStatus、code 與 message，防止誤改。
 */
class ResultStatusTest {

    @Test
    void successConstant() {
        assertEquals(HttpStatus.OK, ResultStatus.SUCCESS.getHttpStatus());
        assertEquals(200, ResultStatus.SUCCESS.getCode());
        assertEquals("OK", ResultStatus.SUCCESS.getMessage());
    }

    @Test
    void badRequestConstant() {
        assertEquals(HttpStatus.BAD_REQUEST, ResultStatus.BAD_REQUEST.getHttpStatus());
        assertEquals(400, ResultStatus.BAD_REQUEST.getCode());
        assertEquals("Bad Request", ResultStatus.BAD_REQUEST.getMessage());
    }

    @Test
    void internalServerErrorConstant() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ResultStatus.INTERNAL_SERVER_ERROR.getHttpStatus());
        assertEquals(500, ResultStatus.INTERNAL_SERVER_ERROR.getCode());
        assertEquals("Internal Server Error", ResultStatus.INTERNAL_SERVER_ERROR.getMessage());
    }
}
