package com.resto.unit;

import com.resto.audit.service.AuditService;
import com.resto.catalog.domain.ModifierGroup;
import com.resto.catalog.domain.ModifierOption;
import com.resto.catalog.domain.Product;
import com.resto.catalog.domain.ProductModifierGroup;
import com.resto.catalog.domain.StoreProduct;
import com.resto.catalog.dto.ResolvedProductDto;
import com.resto.catalog.repository.ModifierGroupRepository;
import com.resto.catalog.repository.ModifierOptionRepository;
import com.resto.catalog.repository.ProductModifierGroupRepository;
import com.resto.catalog.repository.ProductRepository;
import com.resto.catalog.repository.StoreProductRepository;
import com.resto.catalog.service.StoreCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreCatalogServiceTest {

    @Mock ProductRepository productRepository;
    @Mock StoreProductRepository storeProductRepository;
    @Mock ProductModifierGroupRepository productModifierGroupRepository;
    @Mock ModifierGroupRepository modifierGroupRepository;
    @Mock ModifierOptionRepository modifierOptionRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock AuditService auditService;

    StoreCatalogService service;

    UUID orgId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StoreCatalogService(
                productRepository,
                storeProductRepository,
                productModifierGroupRepository,
                modifierGroupRepository,
                modifierOptionRepository,
                messagingTemplate,
                auditService
        );
    }

    @Test
    @DisplayName("Resolved catalog applies store override price and modifier groups")
    void resolvedModifiersAndOverride() {
        Product product = Product.builder()
                .id(productId)
                .organizationId(orgId)
                .categoryId(categoryId)
                .name("Burger")
                .description("Classic")
                .basePrice(new BigDecimal("10.00"))
                .taxRate(new BigDecimal("10.00"))
                .active(true)
                .build();

        StoreProduct override = StoreProduct.builder()
                .organizationId(orgId)
                .storeId(storeId)
                .productId(productId)
                .overridePrice(new BigDecimal("11.50"))
                .available(true)
                .build();

        ProductModifierGroup link = new ProductModifierGroup();
        link.setProductId(productId);
        link.setModifierGroupId(groupId);
        link.setOrganizationId(orgId);
        link.setDisplayOrder(1);

        ModifierGroup group = ModifierGroup.builder()
                .id(groupId)
                .organizationId(orgId)
                .name("Extras")
                .required(false)
                .minSelection(0)
                .maxSelection(2)
                .build();

        ModifierOption option = ModifierOption.builder()
                .id(optionId)
                .organizationId(orgId)
                .modifierGroupId(groupId)
                .name("Cheese")
                .priceDelta(new BigDecimal("1.00"))
                .displayOrder(0)
                .build();

        when(productRepository.findByOrganizationId(orgId)).thenReturn(List.of(product));
        when(storeProductRepository.findByStoreId(storeId)).thenReturn(List.of(override));
        when(productModifierGroupRepository.findByOrganizationId(orgId)).thenReturn(List.of(link));
        when(modifierGroupRepository.findAllById(anyList())).thenReturn(List.of(group));
        when(modifierOptionRepository.findByModifierGroupIdInOrderByDisplayOrderAsc(anyList()))
                .thenReturn(List.of(option));

        List<ResolvedProductDto> catalog = service.getResolvedCatalogForStore(orgId, storeId);

        assertEquals(1, catalog.size());
        ResolvedProductDto dto = catalog.get(0);
        assertEquals(productId, dto.getProductId());
        assertEquals(new BigDecimal("10.00"), dto.getBasePrice());
        assertEquals(new BigDecimal("11.50"), dto.getResolvedPrice());
        assertTrue(dto.getAvailable());
        assertFalse(dto.getIs86());
        assertEquals(1, dto.getModifierGroups().size());
        assertEquals("Extras", dto.getModifierGroups().get(0).getName());
        assertEquals(1, dto.getModifierGroups().get(0).getOptions().size());
        assertEquals("Cheese", dto.getModifierGroups().get(0).getOptions().get(0).getName());
        assertEquals(new BigDecimal("1.00"), dto.getModifierGroups().get(0).getOptions().get(0).getPriceDelta());
    }

    @Test
    @DisplayName("Store unavailable marks product as 86")
    void storeUnavailableIs86() {
        Product product = Product.builder()
                .id(productId)
                .organizationId(orgId)
                .categoryId(categoryId)
                .name("Soup")
                .basePrice(new BigDecimal("5.00"))
                .taxRate(new BigDecimal("10.00"))
                .active(true)
                .build();
        StoreProduct override = StoreProduct.builder()
                .organizationId(orgId)
                .storeId(storeId)
                .productId(productId)
                .available(false)
                .build();

        when(productRepository.findByOrganizationId(orgId)).thenReturn(List.of(product));
        when(storeProductRepository.findByStoreId(storeId)).thenReturn(List.of(override));
        when(productModifierGroupRepository.findByOrganizationId(orgId)).thenReturn(List.of());

        List<ResolvedProductDto> catalog = service.getResolvedCatalogForStore(orgId, storeId);
        assertEquals(1, catalog.size());
        assertTrue(catalog.get(0).getIs86());
        assertFalse(catalog.get(0).getAvailable());
        assertTrue(catalog.get(0).getModifierGroups().isEmpty());
    }
}
