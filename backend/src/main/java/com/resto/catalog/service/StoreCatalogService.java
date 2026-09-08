package com.resto.catalog.service;

import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.dto.ResolvedProductDto;
import com.resto.catalog.repository.ProductRepository;
import com.resto.catalog.repository.StoreProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreCatalogService {

    private final ProductRepository productRepository;
    private final StoreProductRepository storeProductRepository;

    public StoreCatalogService(ProductRepository productRepository, StoreProductRepository storeProductRepository) {
        this.productRepository = productRepository;
        this.storeProductRepository = storeProductRepository;
    }

    public StoreProduct setStoreOverride(UUID organizationId, UUID storeId, UUID productId, BigDecimal overridePrice, Boolean available) {
        StoreProduct storeProduct = storeProductRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseGet(() -> StoreProduct.builder()
                        .organizationId(organizationId)
                        .storeId(storeId)
                        .productId(productId)
                        .build());

        if (overridePrice != null) {
            storeProduct.setOverridePrice(overridePrice);
        }
        if (available != null) {
            storeProduct.setAvailable(available);
        }

        return storeProductRepository.save(storeProduct);
    }

    @Transactional(readOnly = true)
    public List<ResolvedProductDto> getResolvedCatalogForStore(UUID organizationId, UUID storeId) {
        List<Product> products = productRepository.findByOrganizationId(organizationId);
        List<StoreProduct> storeOverrides = storeProductRepository.findByStoreId(storeId);

        Map<UUID, StoreProduct> overrideMap = storeOverrides.stream()
                .collect(Collectors.toMap(StoreProduct::getProductId, Function.identity()));

        List<ResolvedProductDto> result = new ArrayList<>();
        for (Product product : products) {
            StoreProduct override = overrideMap.get(product.getId());
            BigDecimal resolvedPrice = (override != null && override.getOverridePrice() != null)
                    ? override.getOverridePrice()
                    : product.getBasePrice();
            boolean isAvailable = (override == null || Boolean.TRUE.equals(override.getAvailable()))
                    && Boolean.TRUE.equals(product.getActive())
                    && !Boolean.TRUE.equals(product.getIs86());

            result.add(ResolvedProductDto.builder()
                    .productId(product.getId())
                    .categoryId(product.getCategoryId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .basePrice(product.getBasePrice())
                    .resolvedPrice(resolvedPrice)
                    .taxRate(product.getTaxRate())
                    .imageUrl(product.getImageUrl())
                    .is86(product.getIs86())
                    .available(isAvailable)
                    .build());
        }

        return result;
    }
}
