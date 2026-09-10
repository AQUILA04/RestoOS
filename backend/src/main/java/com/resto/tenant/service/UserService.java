package com.resto.tenant.service;

import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.MembershipStore;
import com.resto.tenant.domain.User;
import com.resto.tenant.dto.OrgMemberDto;
import com.resto.tenant.repository.MembershipRepository;
import com.resto.tenant.repository.MembershipStoreRepository;
import com.resto.tenant.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
            "OWNER", "ADMIN", "STORE_MANAGER", "CASHIER", "WAITER", "KITCHEN");

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

    /**
     * Finds an existing user by email or creates a placeholder user record.
     * Used by the invite flow to resolve an email to a User ID before Keycloak onboarding.
     */
    public User findOrCreateUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .email(email)
                            .build();
                    return userRepository.save(user);
                });
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

    @Transactional(readOnly = true)
    public List<OrgMemberDto> listOrgMembers(UUID organizationId) {
        return membershipRepository.findByOrganizationId(organizationId).stream()
                .map(m -> {
                    User user = userRepository.findById(m.getUserId()).orElse(null);
                    OrgMemberDto dto = new OrgMemberDto();
                    dto.setMembershipId(m.getId());
                    dto.setUserId(m.getUserId());
                    dto.setRole(m.getRole());
                    if (user != null) {
                        dto.setEmail(user.getEmail());
                        dto.setFirstName(user.getFirstName());
                        dto.setLastName(user.getLastName());
                        dto.setActive(user.getActive());
                        dto.setHasPin(user.getPinHash() != null && !user.getPinHash().isBlank());
                    } else {
                        dto.setActive(false);
                        dto.setHasPin(false);
                    }
                    dto.setStoreIds(membershipStoreRepository.findByMembershipId(m.getId()).stream()
                            .map(MembershipStore::getStoreId)
                            .toList());
                    return dto;
                })
                .toList();
    }

    public Membership updateMembershipRole(UUID membershipId, String role) {
        if (role == null || !ASSIGNABLE_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        membership.setRole(role);
        return membershipRepository.save(membership);
    }

    public User setUserActive(UUID userId, boolean active) {
        User user = getUserById(userId);
        user.setActive(active);
        return userRepository.save(user);
    }

    public Membership replaceMembershipStores(UUID membershipId, List<UUID> storeIds) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found: " + membershipId));
        List<MembershipStore> existing = membershipStoreRepository.findByMembershipId(membershipId);
        if (!existing.isEmpty()) {
            membershipStoreRepository.deleteAll(existing);
            membershipStoreRepository.flush();
        }
        if (storeIds != null) {
            for (UUID storeId : storeIds.stream().distinct().toList()) {
                MembershipStore ms = MembershipStore.builder()
                        .organizationId(membership.getOrganizationId())
                        .membershipId(membershipId)
                        .storeId(storeId)
                        .build();
                membershipStoreRepository.save(ms);
            }
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public List<User> findUsersByStore(UUID storeId) {
        return membershipStoreRepository.findByStoreId(storeId).stream()
                .map(ms -> membershipRepository.findById(ms.getMembershipId()).orElse(null))
                .filter(m -> m != null)
                .map(m -> userRepository.findById(m.getUserId()).orElse(null))
                .filter(u -> u != null)
                .distinct()
                .toList();
    }
}
