package com.resto.tenant.repository;

import com.resto.tenant.domain.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {
    Optional<InvitationToken> findByToken(String token);
}
