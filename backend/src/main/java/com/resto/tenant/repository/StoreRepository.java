package com.resto.tenant.repository;

import com.resto.tenant.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {
    List<Store> findByOrganizationId(UUID organizationId);
    Optional<Store> findByOrganizationIdAndCode(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}
