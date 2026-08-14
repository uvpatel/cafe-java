package com.cafepos.identity.infrastructure;

public interface PasswordEncodingService {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
