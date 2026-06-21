package com.zipe.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class VerificationTypeEnumTest {

    @Test
    void getEnum_shouldResolveCustomIgnoreCase() {
        assertEquals(VerificationTypeEnum.CUSTOM, VerificationTypeEnum.getEnum("custom"));
        assertEquals(VerificationTypeEnum.CUSTOM, VerificationTypeEnum.getEnum("CUSTOM"));
    }

    @Test
    void getEnum_shouldReturnNullForBlank() {
        assertNull(VerificationTypeEnum.getEnum(" "));
    }
}
