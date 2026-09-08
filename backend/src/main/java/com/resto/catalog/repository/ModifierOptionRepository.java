package com.resto.catalog.repository;

import com.resto.catalog.domain.ModifierOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierOptionRepository extends JpaRepository<ModifierOption, UUID> {
    List<ModifierOption> findByModifierGroupIdOrderByDisplayOrderAsc(UUID modifierGroupId);
}
