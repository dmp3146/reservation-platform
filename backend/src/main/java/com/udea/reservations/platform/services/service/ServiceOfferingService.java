package com.udea.reservations.platform.services.service;

import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceResponse;

public interface ServiceOfferingService {

    ServiceResponse createForProvider(
        Integer authenticatedUserId,
        ServiceCreateRequest request
    );
}
