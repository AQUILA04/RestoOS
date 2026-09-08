package com.resto.tenant.service;

import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.MembershipStore;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.MembershipRepository;
import com.resto.tenant.repository.MembershipStoreRepository;
import com.resto.tenant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipStoreRepository membershipStoreRepository;

    public UserService(UserRepository userRepository,
                       MembershipRepository membershipRepository,
                       MembershipStoreRepository membershipStoreRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.membershipStoreRepository = membershipStoreRepository;
    }

    public User createUser(String email, String firstName, String lastName, String keycloakId) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User email already exists: " + email);
        }
        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .keycloakId(keycloakId)
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public Membership createMembership(UUID organizationId, UUID userId, String role, List<UUID> storeIds) {
        if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, userId)) {
            throw new IllegalArgumentException("Membership already exists for this user in organization.");
        }
        Membership membership = Membership.builder()
                .organizationId(organizationId)
                .userId(userId)
                .role(role)
                .build();
        Membership savedMembership = membershipRepository.save(membership);

        if (storeIds != null) {
            for (UUID storeId : storeIds) {
                MembershipStore ms = MembershipStore.builder()
                        .organizationId(organizationId)
                        .membershipId(savedMembership.getId())
                        .storeId(storeId)
                        .build();
                membershipStoreRepository.save(ms);
            }
        }

        return savedMembership;
    }

    @Transactional(readOnly = true)
    public List<Membership> getMembershipsByUser(UUID userId) {
        return membershipRepository.findByUserId(userId);
    }
}
