package com.zipe.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    @Test
    void defaults_shouldMatchSpec() {
        JwtProperties props = new JwtProperties();
        assertFalse(props.isEnabled());
        assertEquals("HS256", props.getAlgorithm());
        assertEquals(3600L, props.getExpirationSeconds());
        assertEquals("/api/login", props.getLoginUri());
        assertEquals("Authorization", props.getHeader());
        assertEquals("Bearer ", props.getTokenPrefix());
    }
}
