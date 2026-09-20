package com.chetan.taskflow.config;

// <editor-fold defaultstate="collapsed" desc="Shared password hashing policy">
/*
 * Exposes one PasswordEncoder bean backed by BCrypt with its default strength. UserService uses
 * encode when registering; Spring Security uses matches to compare a login password with the saved
 * salted hash. Hashes are one-way, and repeated encoding can produce different hashes for one password.
 */
// </editor-fold>

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}