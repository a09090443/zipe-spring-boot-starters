package com.zipe.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtPropertiesTest {

    @Test
    void defaults_shouldMatchSpec() {
        JwtProperties props = new JwtProperties();
        assertEquals("HS256", props.getAlgorithm());
        assertEquals(3600L, props.getExpirationSeconds());
        assertEquals("/api/login", props.getLoginUri());
        assertEquals("Authorization", props.getHeader());
        assertEquals("Bearer ", props.getTokenPrefix());
    }
}
