package com.udea.reservations.platform.auth.controller;

import com.udea.reservations.platform.auth.dto.CsrfTokenResponse;
import com.udea.reservations.platform.auth.dto.LoginRequest;
import com.udea.reservations.platform.auth.dto.LoginResponse;
import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.auth.service.AuthService;
import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(
        AuthService authService,
        SecurityContextRepository securityContextRepository,
        SessionAuthenticationStrategy sessionAuthenticationStrategy
    ) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfTokenResponse(
            csrfToken.getToken(),
            csrfToken.getHeaderName(),
            csrfToken.getParameterName()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        Authentication authentication = authService.authenticate(request);
        sessionAuthenticationStrategy.onAuthentication(
            authentication,
            httpRequest,
            httpResponse
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(
            securityContext,
            httpRequest,
            httpResponse
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        LoginResponse response = new LoginResponse(
            principal.id(),
            principal.name(),
            principal.email(),
            principal.role()
        );
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Inicio de sesión exitoso.",
            response
        ));
    }
}
