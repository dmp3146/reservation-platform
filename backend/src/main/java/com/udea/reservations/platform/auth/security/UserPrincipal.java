package com.udea.reservations.platform.auth.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(
    Integer id,
    String name,
    String email,
    String passwordHash,
    String role
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return switch (role) {
            case "cliente" -> List.of(
                new SimpleGrantedAuthority("ROLE_CLIENTE")
            );
            case "proveedor" -> List.of(
                new SimpleGrantedAuthority("ROLE_PROVEEDOR")
            );
            case "responsable" -> List.of(
                new SimpleGrantedAuthority("ROLE_RESPONSABLE"),
                new SimpleGrantedAuthority("ROLE_MANAGER")
            );
            default -> List.of();
        };
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "UserPrincipal";
    }
}
