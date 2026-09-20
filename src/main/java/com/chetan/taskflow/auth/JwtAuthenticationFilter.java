package com.chetan.taskflow.auth;

// <editor-fold defaultstate="collapsed" desc="Authenticate an HTTP request using its bearer token">
/*
 * Runs in the security filter chain before username/password authentication. A missing or differently
 * prefixed Authorization header passes onward; SecurityConfig decides whether anonymous access is allowed.
 * A bearer token is verified by JwtService, then its subject is resolved against the current user table.
 * The principal and current role are placed in the request security context, not taken from token roles.
 * Malformed, expired, empty or unknown-user tokens end the request with INVALID_TOKEN and HTTP 401.
 * This filter writes its own JSON because controller advice does not handle these filter-stage errors.
 */
// </editor-fold>

import com.chetan.taskflow.config.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // <editor-fold defaultstate="collapsed" desc="Validate the bearer credential">
        /*
         * Seven characters remove exactly the Bearer prefix and its space. Even an empty remainder
         * is passed to the parser, whose IllegalArgumentException is handled below. Successful parsing
         * precedes user lookup. An existing authentication is preserved rather than replaced.
         */
        // </editor-fold>
        String token = authHeader.substring(7);
        try {
            String email = jwtService.extractEmail(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        // <editor-fold defaultstate="collapsed" desc="Stop invalid requests before controllers execute">
        /*
         * JwtException here comes from io.jsonwebtoken, the library used by JwtService. It covers
         * signature, expiry and format failures. UsernameNotFoundException covers removed accounts.
         * Returning after writing JSON prevents downstream code from executing on a rejected token.
         */
        // </editor-fold>
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write(
                    """
                    {
                      "error": "INVALID_TOKEN",
                      "message": "Invalid or expired authentication token"
                    }
                    """
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}
