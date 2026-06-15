package com.zipe.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VerificationTypeEnumTest {

    @Test
    void getEnum_shouldResolveJwtIgnoreCase() {
        assertEquals(VerificationTypeEnum.JWT, VerificationTypeEnum.getEnum("jwt"));
        assertEquals(VerificationTypeEnum.JWT, VerificationTypeEnum.getEnum("JWT"));
    }

    @Test
    void getEnum_shouldReturnNullForBlank() {
        assertNull(VerificationTypeEnum.getEnum(" "));
    }
}
