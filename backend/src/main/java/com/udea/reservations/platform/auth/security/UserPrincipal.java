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
        String authority = switch (role) {
            case "cliente" -> "ROLE_CLIENTE";
            case "proveedor" -> "ROLE_PROVEEDOR";
            case "responsable" -> "ROLE_RESPONSABLE";
            default -> null;
        };
        return authority == null
            ? List.of()
            : List.of(new SimpleGrantedAuthority(authority));
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
