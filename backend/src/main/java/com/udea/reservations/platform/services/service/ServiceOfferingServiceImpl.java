package com.udea.reservations.platform.services.service;

import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceResponse;
import com.udea.reservations.platform.services.exception.ProviderProfileNotFoundException;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceOfferingServiceImpl implements ServiceOfferingService {

    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;

    @Override
    @Transactional
    public ServiceResponse createForProvider(
        Integer authenticatedUserId,
        ServiceCreateRequest request
    ) {
        Provider provider = providerRepository.findByUser_Id(authenticatedUserId)
            .orElseThrow(ProviderProfileNotFoundException::new);

        ServiceOffering offering = new ServiceOffering();
        offering.setProvider(provider);
        offering.setName(request.nombre());
        offering.setDescription(request.descripcion());
        offering.setDurationMinutes(request.duracionMinutos());
        offering.setActive(true);

        ServiceOffering saved = serviceRepository.save(offering);
        return new ServiceResponse(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            saved.getDurationMinutes(),
            saved.getActive()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponse> findForProvider(Integer authenticatedUserId) {
        return serviceRepository
            .findAllByProvider_User_IdOrderByIdAsc(authenticatedUserId)
            .stream()
            .map(offering -> new ServiceResponse(
                offering.getId(),
                offering.getName(),
                offering.getDescription(),
                offering.getDurationMinutes(),
                offering.getActive()
            ))
            .toList();
    }
}
