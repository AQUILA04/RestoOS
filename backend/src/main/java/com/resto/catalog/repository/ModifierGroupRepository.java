package com.resto.catalog.repository;

import com.resto.catalog.domain.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {
    List<ModifierGroup> findByOrganizationId(UUID organizationId);
}
