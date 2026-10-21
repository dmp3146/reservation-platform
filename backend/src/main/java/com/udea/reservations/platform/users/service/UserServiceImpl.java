package com.udea.reservations.platform.users.service;

import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.exception.EmailAlreadyRegisteredException;
import com.udea.reservations.platform.users.exception.PasswordMismatchException;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String CLIENT_ROLE = "cliente";
    private static final String PROVIDER_ROLE = "proveedor";
    private static final String EMAIL_UNIQUE_CONSTRAINT = "usuario_email_key";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest registrationRequest) {
        User user = createAccount(registrationRequest, CLIENT_ROLE);
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public User registerProviderAccount(
        UserRegistrationRequest registrationRequest
    ) {
        return createAccount(registrationRequest, PROVIDER_ROLE);
    }

    private User createAccount(
        UserRegistrationRequest registrationRequest,
        String role
    ) {
        if (
            !registrationRequest.password().equals(
                registrationRequest.confirmPassword()
            )
        ) {
            throw new PasswordMismatchException();
        }

        String normalizedEmail = registrationRequest.email()
            .trim()
            .toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User();
        user.setName(registrationRequest.name());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
            passwordEncoder.encode(registrationRequest.password())
        );
        user.setRole(role);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueViolation(exception)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole()
        );
    }

    private static boolean isEmailUniqueViolation(
        DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (
                cause instanceof ConstraintViolationException violation
                && EMAIL_UNIQUE_CONSTRAINT.equals(
                    violation.getConstraintName()
                )
            ) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
