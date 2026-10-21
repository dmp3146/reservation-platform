package com.udea.reservations.platform.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.exception.EmailAlreadyRegisteredException;
import com.udea.reservations.platform.users.exception.PasswordMismatchException;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

    @Test
    void registerStoresNormalizedEmailClientRoleAndEncodedPassword() {
        RepositoryProbe repository = new RepositoryProbe();
        PasswordEncoderProbe passwordEncoder = new PasswordEncoderProbe();
        UserServiceImpl userService = new UserServiceImpl(
            repository.createRepository(),
            passwordEncoder
        );
        UserRegistrationRequest request = validRequest(
            "  Ana Pérez  ",
            " ANA@EXAMPLE.COM ",
            "clave-segura-1",
            "clave-segura-1"
        );

        UserResponse response = userService.register(request);

        User savedUser = repository.savedUser;
        assertThat(savedUser.getName()).isEqualTo("  Ana Pérez  ");
        assertThat(savedUser.getEmail()).isEqualTo("ana@example.com");
        assertThat(savedUser.getRole()).isEqualTo("cliente");
        assertThat(passwordEncoder.matches(
            "clave-segura-1",
            savedUser.getPasswordHash()
        )).isTrue();
        assertThat(response).isEqualTo(
            new UserResponse(12, "  Ana Pérez  ", "ana@example.com", "cliente")
        );
        assertThat(passwordEncoder.encodeCalls).isEqualTo(1);
    }

    @Test
    void registerProviderAccountStoresProviderRoleUsingSharedRegistrationLogic() {
        RepositoryProbe repository = new RepositoryProbe();
        PasswordEncoderProbe passwordEncoder = new PasswordEncoderProbe();
        UserServiceImpl userService = new UserServiceImpl(
            repository.createRepository(),
            passwordEncoder
        );

        User user = userService.registerProviderAccount(
            validRequest(
                "Ana Pérez",
                " ANA@EXAMPLE.COM ",
                "clave-segura-1",
                "clave-segura-1"
            )
        );

        assertThat(user).isSameAs(repository.savedUser);
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getRole()).isEqualTo("proveedor");
        assertThat(passwordEncoder.matches(
            "clave-segura-1",
            user.getPasswordHash()
        )).isTrue();
        assertThat(passwordEncoder.encodeCalls).isEqualTo(1);
    }

    @Test
    void registerRejectsExistingEmailBeforeEncodingOrSaving() {
        RepositoryProbe repository = new RepositoryProbe();
        repository.existingEmail = "ana@example.com";
        PasswordEncoderProbe passwordEncoder = new PasswordEncoderProbe();
        UserServiceImpl userService = new UserServiceImpl(
            repository.createRepository(),
            passwordEncoder
        );

        assertThatThrownBy(() -> userService.register(validRequest()))
            .isInstanceOf(EmailAlreadyRegisteredException.class)
            .hasMessage("El correo electrónico ya está registrado.");

        assertThat(repository.savedUser).isNull();
        assertThat(passwordEncoder.encodeCalls).isZero();
    }

    @Test
    void registerRejectsDifferentPasswordsBeforeRepositoryAccess() {
        RepositoryProbe repository = new RepositoryProbe();
        PasswordEncoderProbe passwordEncoder = new PasswordEncoderProbe();
        UserServiceImpl userService = new UserServiceImpl(
            repository.createRepository(),
            passwordEncoder
        );
        UserRegistrationRequest request = validRequest(
            "Ana Pérez",
            "ana@example.com",
            "clave-segura-1",
            "otra-clave-2"
        );

        assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(PasswordMismatchException.class)
            .hasMessage("Las contraseñas no coinciden.")
            .satisfies(exception ->
                assertThat(((PasswordMismatchException) exception).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST)
            );

        assertThat(repository.existenceChecks).isZero();
        assertThat(repository.savedUser).isNull();
        assertThat(passwordEncoder.encodeCalls).isZero();
    }

    @Test
    void registerTranslatesEmailUniqueConstraintViolationFromDatabase() {
        RepositoryProbe repository = new RepositoryProbe();
        ConstraintViolationException constraintViolation =
            new ConstraintViolationException(
                "duplicate email",
                null,
                "usuario_email_key"
            );
        repository.saveFailure = new DataIntegrityViolationException(
            "Unique email constraint violated",
            constraintViolation
        );
        UserServiceImpl userService = new UserServiceImpl(
            repository.createRepository(),
            new PasswordEncoderProbe()
        );

        assertThatThrownBy(() -> userService.register(validRequest()))
            .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void registerRethrowsUnrelatedPersistenceConstraintViolation() {
        RepositoryProbe repository = new RepositoryProbe();
        ConstraintViolationException constraintViolation =
            new ConstraintViolationException(
                "other constraint",
                null,
                "usuario_rol_check"
            );
        DataIntegrityViolationException persistenceException =
            new DataIntegrityViolationException(
                "Other constraint violated",
                constraintViolation
            );
        repository.saveFailure = persistenceException;
        UserServiceImpl userService = new UserServiceImpl(
            repository.createRepository(),
            new PasswordEncoderProbe()
        );

        assertThatThrownBy(() -> userService.register(validRequest()))
            .isSameAs(persistenceException);
    }

    private static UserRegistrationRequest validRequest() {
        return validRequest(
            "Ana Pérez",
            "ana@example.com",
            "clave-segura-1",
            "clave-segura-1"
        );
    }

    private static UserRegistrationRequest validRequest(
        String name,
        String email,
        String password,
        String confirmPassword
    ) {
        return new UserRegistrationRequest(
            name,
            email,
            password,
            confirmPassword
        );
    }

    private static final class RepositoryProbe implements InvocationHandler {

        private String existingEmail;
        private User savedUser;
        private RuntimeException saveFailure;
        private int existenceChecks;

        private UserRepository createRepository() {
            return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[] { UserRepository.class },
                this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getName().equals("existsByEmailIgnoreCase")) {
                existenceChecks++;
                return existingEmail != null
                    && existingEmail.equalsIgnoreCase((String) arguments[0]);
            }
            if (method.getName().equals("saveAndFlush")) {
                if (saveFailure != null) {
                    throw saveFailure;
                }
                savedUser = (User) arguments[0];
                savedUser.setId(12);
                return savedUser;
            }
            throw new UnsupportedOperationException(method.toString());
        }
    }

    private static final class PasswordEncoderProbe implements PasswordEncoder {

        private final PasswordEncoder delegate =
            new com.udea.reservations.platform.users.config.PasswordConfig()
                .passwordEncoder();
        private int encodeCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            encodeCalls++;
            return delegate.encode(rawPassword);
        }

        @Override
        public boolean matches(
            CharSequence rawPassword,
            String encodedPassword
        ) {
            return delegate.matches(rawPassword, encodedPassword);
        }
    }
}
