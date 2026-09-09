package com.resto.unit;

import com.resto.tenant.domain.InvitationToken;
import com.resto.tenant.domain.Membership;
import com.resto.tenant.domain.User;
import com.resto.tenant.repository.InvitationTokenRepository;
import com.resto.tenant.repository.UserRepository;
import com.resto.tenant.service.InvitationService;
import com.resto.tenant.service.UserService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock UserService userService;
    @Mock UserRepository userRepository;
    @Mock InvitationTokenRepository invitationTokenRepository;
    @Mock JavaMailSender mailSender;

    InvitationService invitationService;

    UUID orgId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(
                userService,
                userRepository,
                invitationTokenRepository,
                mailSender,
                "http://localhost:4200/activate",
                "noreply@restoos.local"
        );
    }

    @Test
    @DisplayName("Invite sets user inactive and sends HTML with Activer mon compte")
    void inviteSetsInactiveAndSendsHtml() throws Exception {
        User user = User.builder().id(userId).email("chef@resto.test").active(true).build();
        Membership membership = Membership.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .userId(userId)
                .role("STAFF")
                .build();

        when(userService.findOrCreateUserByEmail("chef@resto.test")).thenReturn(user);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userService.createMembership(eq(orgId), eq(userId), eq("STAFF"), anyList()))
                .thenReturn(membership);
        when(invitationTokenRepository.save(any(InvitationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        Membership result = invitationService.invite(orgId, "chef@resto.test", "STAFF", List.of());

        assertSame(membership, result);
        assertFalse(user.getActive());
        verify(userRepository).save(user);

        ArgumentCaptor<InvitationToken> tokenCap = ArgumentCaptor.forClass(InvitationToken.class);
        verify(invitationTokenRepository).save(tokenCap.capture());
        assertEquals(orgId, tokenCap.getValue().getOrganizationId());
        assertEquals(userId, tokenCap.getValue().getUserId());
        assertFalse(tokenCap.getValue().getConsumed());

        verify(mailSender).send(mimeMessage);
        String html = extractHtml(mimeMessage);
        assertTrue(html.contains("Activer mon compte"), "HTML should contain activation CTA: " + html);
        assertTrue(html.contains("token=" + tokenCap.getValue().getToken()), html);
    }

    @Test
    @DisplayName("Activate consumes token and activates user")
    void activateConsumesToken() {
        String token = UUID.randomUUID().toString();
        InvitationToken invitation = new InvitationToken();
        invitation.setId(UUID.randomUUID());
        invitation.setOrganizationId(orgId);
        invitation.setUserId(userId);
        invitation.setEmail("chef@resto.test");
        invitation.setToken(token);
        invitation.setConsumed(false);

        User user = User.builder().id(userId).email("chef@resto.test").active(false).build();

        when(invitationTokenRepository.findByToken(token)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invitationTokenRepository.save(any(InvitationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = invitationService.activate(token);

        assertEquals(true, result.get("activated"));
        assertEquals(userId.toString(), result.get("userId"));
        assertTrue(user.getActive());
        assertTrue(invitation.getConsumed());
        verify(invitationTokenRepository).save(invitation);
    }

    @Test
    @DisplayName("Activate rejects already consumed token")
    void activateRejectsConsumed() {
        InvitationToken invitation = new InvitationToken();
        invitation.setToken("used");
        invitation.setConsumed(true);
        when(invitationTokenRepository.findByToken("used")).thenReturn(Optional.of(invitation));

        assertThrows(IllegalStateException.class, () -> invitationService.activate("used"));
    }

    private static String extractHtml(MimeMessage message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof jakarta.mail.Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                Object part = multipart.getBodyPart(i).getContent();
                if (part instanceof String s) {
                    sb.append(s);
                }
            }
            return sb.toString();
        }
        return String.valueOf(content);
    }
}
