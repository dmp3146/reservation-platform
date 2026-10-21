package com.udea.reservations.platform.providers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.exception.EmailAlreadyRegisteredException;
import com.udea.reservations.platform.users.exception.PasswordMismatchException;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.service.UserService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ProviderServiceImplTest {

    @Test
    void registerProviderSavesProfileAndReturnsOnlyPublicFields() {
        User user = providerAccount();
        UserServiceProbe userService = new UserServiceProbe(user);
        ProviderRepositoryProbe repository = new ProviderRepositoryProbe();
        ProviderService providerService = new ProviderServiceImpl(
            userService,
            repository.createRepository()
        );
        ProviderRegistrationRequest request = validRequest();

        ProviderRegistrationResponse response =
            providerService.registerProvider(request);

        assertThat(userService.request).isEqualTo(
            new UserRegistrationRequest(
                request.name(),
                request.email(),
                request.password(),
                request.confirmPassword()
            )
        );
        assertThat(repository.savedProvider.getUser()).isSameAs(user);
        assertThat(repository.savedProvider.getBusinessName())
            .isEqualTo("Café Ana");
        assertThat(repository.savedProvider.getPhone()).isNull();
        assertThat(response).isEqualTo(
            new ProviderRegistrationResponse(
                21,
                "Ana Pérez",
                "Café Ana",
                "ana@example.com",
                null
            )
        );
    }

    @Test
    void registerProviderDoesNotSaveProfileWhenEmailIsAlreadyRegistered() {
        EmailAlreadyRegisteredException failure =
            new EmailAlreadyRegisteredException();
        UserServiceProbe userService = new UserServiceProbe(failure);
        ProviderRepositoryProbe repository = new ProviderRepositoryProbe();
        ProviderService providerService = new ProviderServiceImpl(
            userService,
            repository.createRepository()
        );

        assertThatThrownBy(() ->
            providerService.registerProvider(validRequest())
        ).isSameAs(failure);

        assertThat(repository.savedProvider).isNull();
    }

    @Test
    void registerProviderDoesNotSaveProfileWhenPasswordsDoNotMatch() {
        PasswordMismatchException failure = new PasswordMismatchException();
        UserServiceProbe userService = new UserServiceProbe(failure);
        ProviderRepositoryProbe repository = new ProviderRepositoryProbe();
        ProviderService providerService = new ProviderServiceImpl(
            userService,
            repository.createRepository()
        );
        ProviderRegistrationRequest request = new ProviderRegistrationRequest(
            "Ana Pérez",
            "ana@example.com",
            "clave-segura-1",
            "otra-clave-2",
            "Café Ana",
            null
        );

        assertThatThrownBy(() ->
            providerService.registerProvider(request)
        ).isSameAs(failure);

        assertThat(repository.savedProvider).isNull();
    }

    @Test
    void registerProviderPropagatesProfilePersistenceFailure() {
        UserServiceProbe userService = new UserServiceProbe(providerAccount());
        ProviderRepositoryProbe repository = new ProviderRepositoryProbe();
        DataIntegrityViolationException failure =
            new DataIntegrityViolationException("provider constraint failure");
        repository.saveFailure = failure;
        ProviderService providerService = new ProviderServiceImpl(
            userService,
            repository.createRepository()
        );

        assertThatThrownBy(() ->
            providerService.registerProvider(validRequest())
        ).isSameAs(failure);

        assertThat(userService.request).isNotNull();
    }

    private static ProviderRegistrationRequest validRequest() {
        return new ProviderRegistrationRequest(
            "Ana Pérez",
            " ANA@EXAMPLE.COM ",
            "clave-segura-1",
            "clave-segura-1",
            "Café Ana",
            null
        );
    }

    private static User providerAccount() {
        User user = new User();
        user.setId(12);
        user.setName("Ana Pérez");
        user.setEmail("ana@example.com");
        user.setPasswordHash("encoded-secret");
        user.setRole("proveedor");
        return user;
    }

    private static final class UserServiceProbe implements UserService {

        private final User user;
        private final RuntimeException failure;
        private UserRegistrationRequest request;

        private UserServiceProbe(User user) {
            this.user = user;
            this.failure = null;
        }

        private UserServiceProbe(RuntimeException failure) {
            this.user = null;
            this.failure = failure;
        }

        @Override
        public UserResponse register(UserRegistrationRequest request) {
            throw new UnsupportedOperationException(
                "Client registration is not used by this test."
            );
        }

        @Override
        public User registerProviderAccount(
            UserRegistrationRequest registrationRequest
        ) {
            request = registrationRequest;
            if (failure != null) {
                throw failure;
            }
            return user;
        }
    }

    private static final class ProviderRepositoryProbe
        implements InvocationHandler {

        private Provider savedProvider;
        private RuntimeException saveFailure;

        private ProviderRepository createRepository() {
            return (ProviderRepository) Proxy.newProxyInstance(
                ProviderRepository.class.getClassLoader(),
                new Class<?>[] { ProviderRepository.class },
                this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getName().equals("saveAndFlush")) {
                if (saveFailure != null) {
                    throw saveFailure;
                }
                savedProvider = (Provider) arguments[0];
                savedProvider.setId(21);
                return savedProvider;
            }
            throw new UnsupportedOperationException(method.toString());
        }
    }
}
