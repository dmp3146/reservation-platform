package com.udea.reservations.platform.services.service;

import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceResponse;
import com.udea.reservations.platform.services.dto.ServiceAvailabilityResponse;
import java.util.List;

public interface ServiceOfferingService {

    ServiceResponse createForProvider(
        Integer authenticatedUserId,
        ServiceCreateRequest request
    );

    List<ServiceResponse> findForProvider(Integer authenticatedUserId);

    List<ServiceAvailabilityResponse> findAvailableServices();

    List<ServiceAvailabilityResponse> findActiveServicesByProvider(
        Integer providerId
    );
}
