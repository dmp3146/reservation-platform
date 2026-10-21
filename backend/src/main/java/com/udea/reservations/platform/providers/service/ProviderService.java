package com.udea.reservations.platform.providers.service;

import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.dto.ProviderPageResponse;

public interface ProviderService {

    ProviderRegistrationResponse registerProvider(
        ProviderRegistrationRequest registrationRequest
    );

    ProviderPageResponse findProviders(int page, int size);
}
