package com.chetan.taskflow.user;

// <editor-fold defaultstate="collapsed" desc="Account registration workflow">
/*
 * Lombok injects UserRepository and the shared PasswordEncoder. Registration normalizes email,
 * checks for an existing account, trims the name, hashes the unchanged password and fixes role to
 * USER. Saving supplies the database ID and persistence timestamp before mapping the response.
 * Controller validation is assumed; direct callers do not automatically trigger @Valid checks.
 */
// </editor-fold>

import com.chetan.taskflow.auth.RegisterRequest;
import com.chetan.taskflow.common.exception.EmailAlreadyRegisteredException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // <editor-fold defaultstate="collapsed" desc="Hash credentials and persist a basic account">
    /*
     * Duplicate checking occurs first so an existing identity is not overwritten. BCrypt hashes
     * the original password including any spaces; only name/email are normalized. The role is
     * assigned explicitly to USER, regardless of any extra JSON fields supplied by a caller.
     * Saving triggers User.setCreatedAt and ID generation before the safe response is constructed.
     */
    // </editor-fold>
    public UserResponse register(RegisterRequest request) {

        String normalizedEmail =
                normalizeAndValidateEmail(request.email());

        User user = new User();

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    // <editor-fold defaultstate="collapsed" desc="Normalize identity and reject known duplicates">
    /*
     * Lowercasing with Locale.ROOT avoids locale-specific case rules. The same normalization
     * is used during login. Despite the method name, email format is validated on the request
     * record; this helper checks uniqueness. Concurrent registrations can both pass existsByEmail,
     * so the unique database column remains the final guard; its exception is not mapped here.
     */
    // </editor-fold>
    private String normalizeAndValidateEmail(String email) {
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered"
            );
        }

        return normalizedEmail;
    }

    // <editor-fold defaultstate="collapsed" desc="Exclude the password hash from registration output">
    /*
     * Copy only the five public fields. Returning the entity itself would expose persistence
     * details and risk serializing passwordHash through Lombok-generated getters.
     */
    // </editor-fold>
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}