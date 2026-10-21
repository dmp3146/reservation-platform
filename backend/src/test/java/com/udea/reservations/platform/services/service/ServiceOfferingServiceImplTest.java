package com.udea.reservations.platform.services.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceResponse;
import com.udea.reservations.platform.services.exception.ProviderProfileNotFoundException;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceOfferingServiceImpl service;

    @Test
    void createsServiceForProviderLinkedToAuthenticatedUser() {
        Provider provider = new Provider();
        provider.setId(7);
        ServiceCreateRequest request = new ServiceCreateRequest(
            "Consulta",
            "Consulta inicial",
            45
        );
        when(providerRepository.findByUser_Id(42)).thenReturn(Optional.of(provider));
        when(serviceRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> {
                ServiceOffering offering = invocation.getArgument(0);
                offering.setId(15);
                return offering;
            });

        ServiceResponse response = service.createForProvider(42, request);

        assertThat(response).isEqualTo(
            new ServiceResponse(15, "Consulta", "Consulta inicial", 45, true)
        );
        assertThat(response.toString()).doesNotContain("providerId");
        verify(providerRepository).findByUser_Id(42);
        org.mockito.ArgumentCaptor<ServiceOffering> captor =
            org.mockito.ArgumentCaptor.forClass(ServiceOffering.class);
        verify(serviceRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isSameAs(provider);
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    void rejectsProviderRoleWithoutProviderProfile() {
        when(providerRepository.findByUser_Id(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createForProvider(
            42,
            new ServiceCreateRequest("Consulta", "Consulta inicial", 45)
        ))
            .isInstanceOf(ProviderProfileNotFoundException.class);
        org.mockito.Mockito.verifyNoInteractions(serviceRepository);
    }

    @Test
    void propagatesPersistenceFailures() {
        Provider provider = new Provider();
        when(providerRepository.findByUser_Id(42)).thenReturn(Optional.of(provider));
        RuntimeException failure = new IllegalStateException("database failure");
        when(serviceRepository.save(org.mockito.ArgumentMatchers.any()))
            .thenThrow(failure);

        assertThatThrownBy(() -> service.createForProvider(
            42,
            new ServiceCreateRequest("Consulta", "Consulta inicial", 45)
        )).isSameAs(failure);
    }

    @Test
    void returnsOnlyServicesForAuthenticatedProvider() {
        ServiceOffering active = serviceOffering(15, 7, "Consulta", 45, true);
        ServiceOffering inactive = serviceOffering(16, 7, "Terapia", 60, false);
        when(serviceRepository.findAllByProvider_User_IdOrderByIdAsc(42))
            .thenReturn(List.of(active, inactive));

        List<ServiceResponse> response = service.findForProvider(42);

        assertThat(response).containsExactly(
            new ServiceResponse(15, "Consulta", "Consulta inicial", 45, true),
            new ServiceResponse(16, "Terapia", "Consulta inicial", 60, false)
        );
        verify(serviceRepository).findAllByProvider_User_IdOrderByIdAsc(42);
    }

    @Test
    void returnsEmptyListWhenAuthenticatedProviderHasNoServices() {
        when(serviceRepository.findAllByProvider_User_IdOrderByIdAsc(42))
            .thenReturn(List.of());

        assertThat(service.findForProvider(42)).isEmpty();
        verify(serviceRepository).findAllByProvider_User_IdOrderByIdAsc(42);
    }

    private static ServiceOffering serviceOffering(
        Integer id,
        Integer providerId,
        String name,
        Integer duration,
        boolean active
    ) {
        Provider provider = new Provider();
        provider.setId(providerId);
        ServiceOffering offering = new ServiceOffering();
        offering.setId(id);
        offering.setProvider(provider);
        offering.setName(name);
        offering.setDescription("Consulta inicial");
        offering.setDurationMinutes(duration);
        offering.setActive(active);
        return offering;
    }
}
