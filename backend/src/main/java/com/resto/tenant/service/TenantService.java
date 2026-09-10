package com.resto.tenant.service;

import com.resto.tenant.domain.Organization;
import com.resto.tenant.domain.Store;
import com.resto.tenant.repository.OrganizationRepository;
import com.resto.tenant.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;

    public TenantService(OrganizationRepository organizationRepository, StoreRepository storeRepository) {
        this.organizationRepository = organizationRepository;
        this.storeRepository = storeRepository;
    }

    public Organization createOrganization(String name, String code) {
        if (organizationRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Organization code already exists: " + code);
        }
        Organization org = Organization.builder()
                .name(name)
                .code(code)
                .build();
        return organizationRepository.save(org);
    }

    public Organization updateOrganizationSettings(UUID id, String name, String mobileMoneyLabel, String logoUrl) {
        Organization org = getOrganizationById(id);
        if (name != null) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Organization name is required");
            }
            org.setName(trimmed);
        }
        if (mobileMoneyLabel != null) {
            String trimmed = mobileMoneyLabel.trim();
            org.setMobileMoneyLabel(trimmed.isEmpty() ? null : trimmed);
        }
        if (logoUrl != null) {
            String trimmed = logoUrl.trim();
            org.setLogoUrl(trimmed.isEmpty() ? null : trimmed);
        }
        return organizationRepository.save(org);
    }

    @Transactional(readOnly = true)
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with id: " + id));
    }

    public Store createStore(UUID organizationId, String name, String code, String timezone, String currency) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new IllegalArgumentException("Organization not found with id: " + organizationId);
        }
        if (storeRepository.existsByOrganizationIdAndCode(organizationId, code)) {
            throw new IllegalArgumentException("Store code already exists for this organization: " + code);
        }
        Store store = Store.builder()
                .organizationId(organizationId)
                .name(name)
                .code(code)
                .timezone(timezone != null ? timezone : "UTC")
                .currency(currency != null ? currency : "EUR")
                .build();
        return storeRepository.save(store);
    }

    public Store updateStore(UUID storeId, String name, String timezone, String currency, Boolean active) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found with id: " + storeId));
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Store name is required");
            }
            store.setName(name.trim());
        }
        if (timezone != null && !timezone.isBlank()) {
            store.setTimezone(timezone.trim());
        }
        if (currency != null && !currency.isBlank()) {
            store.setCurrency(currency.trim().toUpperCase());
        }
        if (active != null) {
            store.setActive(active);
        }
        return storeRepository.save(store);
    }

    /** @deprecated use {@link #updateStore} */
    public Store updateStoreName(UUID storeId, String name) {
        return updateStore(storeId, name, null, null, null);
    }

    @Transactional(readOnly = true)
    public Store getStoreById(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found with id: " + storeId));
    }

    @Transactional(readOnly = true)
    public List<Store> getStoresByOrganization(UUID organizationId) {
        return storeRepository.findByOrganizationId(organizationId);
    }
}
