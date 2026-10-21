package com.udea.reservations.platform.auth.service;

import com.udea.reservations.platform.auth.dto.LoginRequest;
import com.udea.reservations.platform.auth.exception.InvalidCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    request.email(),
                    request.password()
                )
            );
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException();
        }
    }
}
