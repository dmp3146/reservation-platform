package com.udea.reservations.platform.auth.security;

import jakarta.servlet.ServletContext;
import jakarta.servlet.Filter;
import java.util.List;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
        CsrfTokenRepository csrfTokenRepository
    ) {
        return new CompositeSessionAuthenticationStrategy(List.of(
            new ChangeSessionIdAuthenticationStrategy(),
            new CsrfAuthenticationStrategy(csrfTokenRepository)
        ));
    }

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(List.of(provider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CsrfTokenRepository csrfTokenRepository,
        SecurityContextRepository securityContextRepository,
        SecurityErrorResponseWriter errorResponseWriter
    ) {
        AccessDeniedHandler csrfDeniedHandler = (request, response, exception) ->
            errorResponseWriter.write(
                response,
                org.springframework.http.HttpStatus.FORBIDDEN,
                "CSRF_INVALID",
                "La solicitud no contiene un token CSRF válido."
            );
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .withObjectPostProcessor(new ObjectPostProcessor<Filter>() {
                    @Override
                    public <O extends Filter> O postProcess(O filter) {
                        if (filter instanceof org.springframework.security.web.csrf.CsrfFilter csrfFilter) {
                            csrfFilter.setAccessDeniedHandler(csrfDeniedHandler);
                        }
                        return filter;
                    }
                })
            )
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId())
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    errorResponseWriter.write(
                        response,
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED",
                        "Debes iniciar sesión para acceder a este recurso."
                    )
                )
                .accessDeniedHandler((request, response, exception) ->
                    errorResponseWriter.write(
                        response,
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "ACCESS_DENIED",
                        "No tienes permiso para acceder a este recurso."
                    )
                )
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/login",
                    "/api/v1/users",
                    "/api/v1/providers"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/services")
                .hasAuthority("ROLE_PROVEEDOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/providers/me/schedules")
                .hasAuthority("ROLE_PROVEEDOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/bookings")
                .hasAuthority("ROLE_CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/bookings/active")
                .hasAuthority("ROLE_CLIENTE")
                .requestMatchers(HttpMethod.GET, "/api/v1/providers/me/services")
                .hasAuthority("ROLE_PROVEEDOR")
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/services/available",
                    "/api/v1/services/*/schedules/available",
                    "/api/v1/providers",
                    "/api/v1/providers/*/services"
                ).authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/services/*/schedules")
                .authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    public ServletContextInitializer sessionCookieConfiguration() {
        return this::configureSessionCookie;
    }

    private void configureSessionCookie(ServletContext servletContext) {
        servletContext.getSessionCookieConfig().setHttpOnly(true);
        servletContext.getSessionCookieConfig().setSecure(true);
        servletContext.getSessionCookieConfig().setAttribute("SameSite", "Lax");
    }
}
