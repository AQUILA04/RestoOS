package com.resto.tenant.service;

import com.resto.tenant.domain.InvitationToken;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.InvitationTokenRepository;
import com.resto.tenant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final JavaMailSender mailSender;
    private final String activationBaseUrl;

    public InvitationService(UserService userService,
                             UserRepository userRepository,
                             InvitationTokenRepository invitationTokenRepository,
                             JavaMailSender mailSender,
                             @Value("${restoos.app.activation-base-url}") String activationBaseUrl) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.invitationTokenRepository = invitationTokenRepository;
        this.mailSender = mailSender;
        this.activationBaseUrl = activationBaseUrl;
    }

    @Transactional
    public Membership invite(UUID organizationId, String email, String role, List<UUID> storeIds) {
        User user = userService.findOrCreateUserByEmail(email);
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

    private void sendInvitationEmail(String email, String token) {
        String link = activationBaseUrl + "?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Invitation à rejoindre RestoOS");
        message.setText(
                "Bonjour,\n\n" +
                "Vous êtes invité(e) à rejoindre RestoOS.\n\n" +
                "Activer mon compte: " + link + "\n\n" +
                "Cordialement,\nL'équipe RestoOS"
        );
        mailSender.send(message);
    }
}
