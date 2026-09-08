package com.resto.tenant.repository;

import com.resto.tenant.domain.MembershipStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembershipStoreRepository extends JpaRepository<MembershipStore, UUID> {
    List<MembershipStore> findByMembershipId(UUID membershipId);
    List<MembershipStore> findByStoreId(UUID storeId);
}
