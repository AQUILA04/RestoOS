package com.resto.catalog.repository;

import com.resto.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByCategoryId(UUID categoryId);
    List<Product> findByOrganizationId(UUID organizationId);
}
