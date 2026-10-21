package com.udea.reservations.platform.services.repository;

import com.udea.reservations.platform.services.model.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository
    extends JpaRepository<ServiceOffering, Integer> {}
