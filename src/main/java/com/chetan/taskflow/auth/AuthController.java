package com.chetan.taskflow.auth;

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

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @Valid @RequestBody LoginRequest request) {

        System.out.println(">>> LOGIN CONTROLLER REACHED <<<");

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        System.out.println(">>> ABOUT TO AUTHENTICATE <<<");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                )
        );

        System.out.println(">>> AUTHENTICATION SUCCESSFUL <<<");

        return ResponseEntity.ok("Login successful");
    }
}