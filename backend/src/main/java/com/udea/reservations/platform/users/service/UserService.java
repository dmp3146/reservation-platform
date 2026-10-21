package com.udea.reservations.platform.users.service;

import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.model.User;

public interface UserService {

    UserResponse register(UserRegistrationRequest registrationRequest);

    User registerProviderAccount(UserRegistrationRequest registrationRequest);
}
