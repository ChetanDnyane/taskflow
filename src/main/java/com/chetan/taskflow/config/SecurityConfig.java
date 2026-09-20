package com.chetan.taskflow.config;

// <editor-fold defaultstate="collapsed" desc="Define request authentication and authorization">
/*
 * This API expects explicit bearer headers. CSRF, form login and HTTP Basic are disabled; stateless
 * session management prevents reuse of authentication from an earlier request. Registration/login
 * POSTs and error dispatches are public; the admin-test path requires ADMIN; everything else requires
 * authentication. Matchers are checked in order. The admin rule does not itself create a controller.
 * Authentication failures return 401 JSON; authenticated callers lacking permission get 403 JSON.
 * The custom JWT filter runs before UsernamePasswordAuthenticationFilter to populate the principal.
 */
// </editor-fold>

import com.chetan.taskflow.auth.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // <editor-fold defaultstate="collapsed" desc="Security bean construction">
    /*
     * Spring invokes these factory methods to supply shared security infrastructure. The filter
     * chain defines request policy; AuthenticationManager is obtained from Spring configuration
     * and used by AuthController for password login with the configured user service/encoder.
     */
    // </editor-fold>
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .formLogin(form -> form.disable())

                .httpBasic(basic -> basic.disable())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");

                            response.getWriter().write(
                                    """
                                    {
                                      "error": "UNAUTHORIZED",
                                      "message": "Authentication is required"
                                    }
                                    """
                            );
                        })

                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");

                            response.getWriter().write(
                                    """
                                    {
                                      "error": "FORBIDDEN",
                                      "message": "You do not have permission to access this resource"
                                    }
                                    """
                            );
                        })
                )

                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/auth/admin-test").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // <editor-fold defaultstate="collapsed" desc="Security bean construction">
    /*
     * Spring invokes these factory methods to supply shared security infrastructure. The filter
     * chain defines request policy; AuthenticationManager is obtained from Spring configuration
     * and used by AuthController for password login with the configured user service/encoder.
     */
    // </editor-fold>
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }
}