package com.resto.tenant.service;

import com.resto.tenant.domain.InvitationToken;
import com.resto.tenant.repository.InvitationTokenRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InvitationMailService {

    private final JavaMailSender mailSender;
    private final InvitationTokenRepository invitationTokenRepository;
    private final String from;
    private final String frontendBaseUrl;

    public InvitationMailService(
            JavaMailSender mailSender,
            InvitationTokenRepository invitationTokenRepository,
            @Value("${restoos.mail.from:noreply@restoos.local}") String from,
            @Value("${restoos.frontend-base-url:http://localhost:4200}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.invitationTokenRepository = invitationTokenRepository;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendInvitation(UUID organizationId, UUID userId, String email) {
        String token = UUID.randomUUID().toString();
        InvitationToken invitation = new InvitationToken();
        invitation.setOrganizationId(organizationId);
        invitation.setUserId(userId);
        invitation.setEmail(email);
        invitation.setToken(token);
        invitation.setConsumed(false);
        invitationTokenRepository.save(invitation);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Invitation à rejoindre RestoOS");
            String link = frontendBaseUrl + "/activate?token=" + token;
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
