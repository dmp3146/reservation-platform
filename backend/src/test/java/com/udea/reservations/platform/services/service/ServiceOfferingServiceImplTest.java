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
            new ServiceResponse(15, "Consulta", "Consulta inicial", 45)
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
}
