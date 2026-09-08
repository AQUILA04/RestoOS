package com.resto.tenant.service;

import com.resto.core.security.TenantContext;
import com.resto.tenant.domain.InvitationToken;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.InvitationTokenRepository;
import com.resto.tenant.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvitationService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final JavaMailSender mailSender;
    private final String activationBaseUrl;
    private final String mailFrom;

    public InvitationService(UserService userService,
                             UserRepository userRepository,
                             InvitationTokenRepository invitationTokenRepository,
                             JavaMailSender mailSender,
                             @Value("${restoos.app.activation-base-url}") String activationBaseUrl,
                             @Value("${restoos.mail.from:noreply@restoos.local}") String mailFrom) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.mailSender = mailSender;
        this.activationBaseUrl = activationBaseUrl;
        this.mailFrom = mailFrom;
    }

    @Transactional
    public Membership invite(UUID organizationId, String email, String role, List<UUID> storeIds) {
        User user = userService.findOrCreateUserByEmail(email);
        user.setActive(false);
        userRepository.save(user);

        Membership membership = userService.createMembership(organizationId, user.getId(), role, storeIds);

        String token = UUID.randomUUID().toString();
        InvitationToken invitation = new InvitationToken();
        invitation.setOrganizationId(organizationId);
        invitation.setUserId(user.getId());
        invitation.setEmail(email);
        invitation.setToken(token);
        invitation.setConsumed(false);
        invitationTokenRepository.save(invitation);

        sendInvitationEmail(email, token);
        return membership;
    }

    @Transactional
    public Map<String, Object> activate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Activation token is required");
        }
        InvitationToken invitation = invitationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid activation token"));
        if (Boolean.TRUE.equals(invitation.getConsumed())) {
            throw new IllegalStateException("Invitation already consumed");
        }

        // Bind RLS tenant context for any follow-on tenant-scoped writes
        TenantContext.setOrgId(invitation.getOrganizationId());

        User user = userRepository.findById(invitation.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found for invitation"));
        user.setActive(true);
        userRepository.save(user);

        invitation.setConsumed(true);
        invitationTokenRepository.save(invitation);

        return Map.of(
                "activated", true,
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "organizationId", invitation.getOrganizationId().toString()
        );
    }

    private void sendInvitationEmail(String email, String token) {
        String base = activationBaseUrl.endsWith("/")
                ? activationBaseUrl.substring(0, activationBaseUrl.length() - 1)
                : activationBaseUrl;
        String link = base.contains("?")
                ? base + "&token=" + token
                : base + "?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Invitation à rejoindre RestoOS");
            helper.setText(
                    "<p>Vous êtes invité à rejoindre RestoOS.</p>"
                            + "<p><a href=\"" + link + "\">Activer mon compte</a></p>",
                    true
            );
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send invitation email", e);
        }
    }
}
