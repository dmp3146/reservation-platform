package com.udea.reservations.platform.users.service;

import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;

public interface UserService {

    UserResponse register(UserRegistrationRequest registrationRequest);
}
