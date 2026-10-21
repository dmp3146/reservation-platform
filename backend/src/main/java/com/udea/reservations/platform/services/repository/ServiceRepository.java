package com.udea.reservations.platform.services.repository;

import com.udea.reservations.platform.services.model.ServiceOffering;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRepository
    extends JpaRepository<ServiceOffering, Integer> {

    List<ServiceOffering> findAllByProvider_User_IdOrderByIdAsc(Integer userId);

    @EntityGraph(attributePaths = "provider")
    List<ServiceOffering> findAllByIdInAndActiveTrueOrderByIdAsc(
        Collection<Integer> ids
    );

    @EntityGraph(attributePaths = "provider")
    List<ServiceOffering> findAllByProvider_IdAndActiveTrueOrderByIdAsc(
        Integer providerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select offering from ServiceOffering offering where offering.id = :id")
    Optional<ServiceOffering> findByIdForUpdate(@Param("id") Integer id);
}
