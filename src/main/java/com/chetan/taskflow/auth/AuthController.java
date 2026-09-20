package com.chetan.taskflow.auth;

// <editor-fold defaultstate="collapsed" desc="Public registration and login endpoints">
/*
 * Spring MVC binds JSON into request records; @Valid rejects invalid fields before the method runs.
 * Registration delegates persistence and password hashing to UserService and returns HTTP 201.
 * Login normalizes email, authenticates the raw password through Spring Security, then issues a JWT.
 * Lombok generates the constructor that injects all three final dependencies. No session is created.
 */
// </editor-fold>

import com.chetan.taskflow.user.UserResponse;
import com.chetan.taskflow.user.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // <editor-fold defaultstate="collapsed" desc="Register and return the public account">
    /*
     * Validation happens before this method; service failures are handled by controller advice.
     * A created account is returned with 201, but registration does not also issue a login token.
     */
    // </editor-fold>
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // <editor-fold defaultstate="collapsed" desc="Verify credentials before minting a token">
    /*
     * Locale.ROOT makes the lookup independent of the machine locale. The authentication manager
     * loads UserDetails and compares the supplied password using the configured PasswordEncoder.
     * If authentication throws, execution stops before token creation. Only a successful login
     * reaches generateToken; the response then contains the signed bearer credential.
     */
    // </editor-fold>
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                )
        );

        String token = jwtService.generateToken(normalizedEmail);

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }
}