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

    @Transactional(readOnly = true)
    public List<Store> getStoresByOrganization(UUID organizationId) {
        return storeRepository.findByOrganizationId(organizationId);
    }
}
