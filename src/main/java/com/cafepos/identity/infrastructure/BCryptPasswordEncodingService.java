package com.cafepos.identity.infrastructure;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BCryptPasswordEncodingService implements PasswordEncodingService {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordEncodingService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public BCryptPasswordEncodingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null || rawPassword.toString().isBlank()) {
            throw new IllegalArgumentException("Raw password must not be null or empty");
        }
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
