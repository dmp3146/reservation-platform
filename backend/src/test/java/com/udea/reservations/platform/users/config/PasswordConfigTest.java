package com.udea.reservations.platform.users.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder =
        new PasswordConfig().passwordEncoder();

    @Test
    void passwordEncoderHashesAndVerifiesPasswordWithinBcryptByteLimit() {
        String password = "ñ".repeat(36);

        String encodedPassword = passwordEncoder.encode(password);

        assertThat(encodedPassword).startsWith("$2");
        assertThat(encodedPassword).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, encodedPassword)).isTrue();
    }

    @Test
    void bcryptRejectsPasswordLongerThan72Utf8Bytes() {
        String password = "ñ".repeat(37);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> passwordEncoder.encode(password));
    }
}
