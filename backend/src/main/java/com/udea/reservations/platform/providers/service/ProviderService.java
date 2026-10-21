package com.udea.reservations.platform.providers.service;

import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;

public interface ProviderService {

    ProviderRegistrationResponse registerProvider(
        ProviderRegistrationRequest registrationRequest
    );
}
