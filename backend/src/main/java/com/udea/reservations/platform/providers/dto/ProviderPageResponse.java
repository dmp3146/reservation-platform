package com.udea.reservations.platform.providers.dto;

import java.util.List;

public record ProviderPageResponse(
    List<ProviderSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
