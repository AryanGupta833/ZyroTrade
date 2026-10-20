package com.ZyroTrade.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTests {
    @Test
    void rejectsMissingOrShortSigningSecret() {
        assertThatThrownBy(() -> new JwtUtil(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> new JwtUtil("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
