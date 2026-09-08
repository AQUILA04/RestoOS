package com.resto.tenant.service;

import com.resto.tenant.domain.User;
import com.resto.tenant.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public boolean validatePin(UUID userId, String rawPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getPinHash() == null) {
            return false;
        }

        return passwordEncoder.matches(rawPin, user.getPinHash());
    }

    public void setPin(UUID userId, String rawPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String hashedPin = passwordEncoder.encode(rawPin);
        user.setPinHash(hashedPin);
        userRepository.save(user);
    }
}
