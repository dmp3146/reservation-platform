package com.udea.reservations.platform.services.service;

import com.udea.reservations.platform.providers.exception.ProviderNotFoundException;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceAvailabilityResponse;
import com.udea.reservations.platform.services.dto.ServiceResponse;
import com.udea.reservations.platform.services.exception.ProviderProfileNotFoundException;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceOfferingServiceImpl implements ServiceOfferingService {

    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;

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

    @Override
    @Transactional(readOnly = true)
    public List<ServiceAvailabilityResponse> findAvailableServices() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Set<Integer> serviceIdsWithFutureSchedules =
            findServiceIdsWithFutureSchedules(now);

        if (serviceIdsWithFutureSchedules.isEmpty()) {
            return List.of();
        }
        return serviceRepository
            .findAllByIdInAndActiveTrueOrderByIdAsc(serviceIdsWithFutureSchedules)
            .stream()
            .map(offering -> toAvailabilityResponse(offering, true))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceAvailabilityResponse> findActiveServicesByProvider(
        Integer providerId
    ) {
        if (!providerRepository.existsById(providerId)) {
            throw new ProviderNotFoundException();
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Set<Integer> serviceIdsWithFutureSchedules = scheduleBlockRepository
            .findServiceIdsWithFutureSchedulesByProviderId(
                providerId,
                now.toLocalDate(),
                now.toLocalTime()
            ).stream().collect(Collectors.toSet());

        return serviceRepository
            .findAllByProvider_IdAndActiveTrueOrderByIdAsc(providerId)
            .stream()
            .map(offering -> toAvailabilityResponse(
                offering,
                serviceIdsWithFutureSchedules.contains(offering.getId())
            ))
            .toList();
    }

    private Set<Integer> findServiceIdsWithFutureSchedules(LocalDateTime now) {
        return scheduleBlockRepository.findServiceIdsWithFutureSchedules(
            now.toLocalDate(),
            now.toLocalTime()
        ).stream().collect(Collectors.toSet());
    }

    private static ServiceAvailabilityResponse toAvailabilityResponse(
        ServiceOffering offering,
        boolean hasFutureSchedules
    ) {
        return new ServiceAvailabilityResponse(
            offering.getId(),
            offering.getName(),
            offering.getDurationMinutes(),
            offering.getProvider().getId(),
            offering.getProvider().getBusinessName(),
            hasFutureSchedules
        );
    }
}
