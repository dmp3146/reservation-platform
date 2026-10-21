package com.udea.reservations.platform.providers.service;

import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final UserService userService;
    private final ProviderRepository providerRepository;

    @Override
    @Transactional
    public ProviderRegistrationResponse registerProvider(
        ProviderRegistrationRequest registrationRequest
    ) {
        User user = userService.registerProviderAccount(
            new UserRegistrationRequest(
                registrationRequest.name(),
                registrationRequest.email(),
                registrationRequest.password(),
                registrationRequest.confirmPassword()
            )
        );

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setBusinessName(registrationRequest.businessName());
        provider.setPhone(registrationRequest.phone());

        Provider savedProvider = providerRepository.saveAndFlush(provider);
        return new ProviderRegistrationResponse(
            savedProvider.getId(),
            user.getName(),
            savedProvider.getBusinessName(),
            user.getEmail(),
            savedProvider.getPhone()
        );
    }
}
