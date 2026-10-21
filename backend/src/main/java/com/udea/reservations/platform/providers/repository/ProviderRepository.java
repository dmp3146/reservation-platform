package com.udea.reservations.platform.providers.repository;

import com.udea.reservations.platform.providers.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderRepository extends JpaRepository<Provider, Integer> {}
