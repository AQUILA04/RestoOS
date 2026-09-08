package com.resto.catalog.repository;

import com.resto.catalog.domain.ProductModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductModifierGroupRepository extends JpaRepository<ProductModifierGroup, ProductModifierGroup.PK> {
    List<ProductModifierGroup> findByProductIdOrderByDisplayOrderAsc(UUID productId);
    List<ProductModifierGroup> findByOrganizationId(UUID organizationId);
    void deleteByProductIdAndModifierGroupId(UUID productId, UUID modifierGroupId);
}
