package com.udea.reservations.platform.providers.dto;

public record ProviderRegistrationResponse(
    Integer providerId,
    String userName,
    String businessName,
    String email,
    String phone
) {}
