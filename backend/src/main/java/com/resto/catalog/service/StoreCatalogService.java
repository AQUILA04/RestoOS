package com.resto.catalog.service;

import com.resto.audit.service.AuditService;
import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.dto.ResolvedProductDto;
import com.resto.catalog.repository.ProductRepository;
import com.resto.catalog.repository.StoreProductRepository;
import com.resto.core.security.TenantContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditService auditService;

    public StoreCatalogService(ProductRepository productRepository,
                               StoreProductRepository storeProductRepository,
                               SimpMessagingTemplate messagingTemplate,
                               AuditService auditService) {
        this.productRepository = productRepository;
        this.storeProductRepository = storeProductRepository;
        this.messagingTemplate = messagingTemplate;
        this.auditService = auditService;
    }

    public StoreProduct setStoreOverride(UUID organizationId, UUID storeId, UUID productId,
                                         BigDecimal overridePrice, Boolean available) {
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

        StoreProduct saved = storeProductRepository.save(storeProduct);
        auditService.record(organizationId, storeId, TenantContext.getUserId(),
                "STORE_PRICE_OVERRIDE", "STORE_PRODUCT", saved.getId(),
                "productId=" + productId + " price=" + overridePrice + " available=" + available);
        return saved;
    }

    /**
     * Store-local 86 / availability. Updates StoreProduct.available (not global Product.is86).
     * Optionally denormalizes Product.is86 when toggling unavailable for display convenience.
     */
    public StoreProduct setAvailability(UUID organizationId, UUID storeId, UUID productId, Boolean available) {
        if (available == null) {
            throw new IllegalArgumentException("available flag is required");
        }
        StoreProduct storeProduct = storeProductRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseGet(() -> StoreProduct.builder()
                        .organizationId(organizationId)
                        .storeId(storeId)
                        .productId(productId)
                        .build());
        storeProduct.setAvailable(available);
        StoreProduct saved = storeProductRepository.save(storeProduct);

        auditService.record(organizationId, storeId, TenantContext.getUserId(),
                "STOCK_86_TOGGLE", "STORE_PRODUCT", saved.getId(),
                "productId=" + productId + " available=" + available);

        afterCommit(() -> messagingTemplate.convertAndSend(
                "/topic/store/" + storeId + "/pos",
                Map.of(
                        "type", "STOCK_86_TOGGLE",
                        "productId", productId.toString(),
                        "available", available,
                        "is86", !available
                )
        ));
        return saved;
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
            boolean storeAvailable = override == null || Boolean.TRUE.equals(override.getAvailable());
            boolean isAvailable = storeAvailable
                    && Boolean.TRUE.equals(product.getActive());

            result.add(ResolvedProductDto.builder()
                    .productId(product.getId())
                    .categoryId(product.getCategoryId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .basePrice(product.getBasePrice())
                    .resolvedPrice(resolvedPrice)
                    .taxRate(product.getTaxRate())
                    .imageUrl(product.getImageUrl())
                    .is86(!storeAvailable)
                    .available(isAvailable)
                    .build());
        }
        return result;
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
