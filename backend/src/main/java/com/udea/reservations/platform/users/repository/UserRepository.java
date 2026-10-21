package com.udea.reservations.platform.users.repository;

import com.udea.reservations.platform.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmailIgnoreCase(String email);
}
