package com.resto.catalog.repository;

import com.resto.catalog.domain.StoreProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreProductRepository extends JpaRepository<StoreProduct, UUID> {
    List<StoreProduct> findByStoreId(UUID storeId);
    Optional<StoreProduct> findByStoreIdAndProductId(UUID storeId, UUID productId);
}
