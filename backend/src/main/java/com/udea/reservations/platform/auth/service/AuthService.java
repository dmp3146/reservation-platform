package com.udea.reservations.platform.auth.service;

import com.udea.reservations.platform.auth.dto.LoginRequest;
import org.springframework.security.core.Authentication;

public interface AuthService {

    Authentication authenticate(LoginRequest request);
}
