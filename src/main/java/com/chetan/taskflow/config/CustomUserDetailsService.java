package com.chetan.taskflow.config;

// <editor-fold defaultstate="collapsed" desc="Bridge persisted users to Spring Security principals">
/*
 * Both password login and bearer-token authentication use this service to look up a normalized email.
 * A missing account throws UsernameNotFoundException. A found account is converted into Spring
 * Security UserDetails: email becomes username, the stored BCrypt hash is used for password checks,
 * and roles(...) prefixes the enum name with ROLE_ so hasRole(...) authorization rules match.
 * Loading on each bearer request means deleted users and changed roles are reflected immediately.
 */
// </editor-fold>

import com.chetan.taskflow.user.User;
import com.chetan.taskflow.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}