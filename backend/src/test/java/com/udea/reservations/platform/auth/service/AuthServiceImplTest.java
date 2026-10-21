package com.udea.reservations.platform.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.auth.dto.LoginRequest;
import com.udea.reservations.platform.auth.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AuthServiceImplTest {

    private final AuthenticationManager authenticationManager =
        org.mockito.Mockito.mock(AuthenticationManager.class);
    private final AuthServiceImpl authService = new AuthServiceImpl(authenticationManager);

    @Test
    void authenticateNormalizesEmailAndPreservesPassword() {
        String password = "  password with spaces  ";
        when(authenticationManager.authenticate(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var result = authService.authenticate(
            new LoginRequest(" ANA@EXAMPLE.COM ", password)
        );

        assertThat(result.getPrincipal()).isEqualTo("ana@example.com");
        assertThat(result.getCredentials()).isEqualTo(password);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateMapsBadCredentialsToGenericBusinessError() {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("invalid"));

        assertThatThrownBy(() -> authService.authenticate(
            new LoginRequest("ana@example.com", "password123")
        ))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("El correo electrónico o la contraseña son incorrectos.");
    }

    @Test
    void authenticateDoesNotHideInfrastructureAuthenticationErrors() {
        InternalAuthenticationServiceException failure =
            new InternalAuthenticationServiceException("database unavailable");
        when(authenticationManager.authenticate(any())).thenThrow(failure);

        assertThatThrownBy(() -> authService.authenticate(
            new LoginRequest("ana@example.com", "password123")
        ))
            .isSameAs(failure);
    }
}
