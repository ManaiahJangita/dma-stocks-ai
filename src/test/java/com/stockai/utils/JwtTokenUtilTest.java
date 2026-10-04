package com.stockai.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenUtilTest {

    @Test
    void generatesAndParsesTokenOnModernJdk() throws ReflectiveOperationException {
        JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();
        setField(jwtTokenUtil, "secretKey", "test-secret");
        setField(jwtTokenUtil, "expirationTimeSeconds", 3600L);

        String token = jwtTokenUtil.generateToken("user@example.com", "42", List.of("ROLE_USER"));

        assertTrue(jwtTokenUtil.validateToken(token));
        assertEquals("user@example.com", jwtTokenUtil.extractUsername(token));
        assertEquals("42", jwtTokenUtil.extractUserId(token));
        assertEquals(List.of("ROLE_USER"), jwtTokenUtil.extractAuthorities(token));
    }

    private static void setField(JwtTokenUtil target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = JwtTokenUtil.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}