package com.philippos.employeemanagement.config;

import com.philippos.employeemanagement.security.JwtAuthenticationFilter;
import com.philippos.employeemanagement.security.CustomAuthenticationEntryPoint;
import com.philippos.employeemanagement.security.CustomAccessDeniedHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http

                // Enable CORS
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                // Disable CSRF because we use JWT
                .csrf(csrf -> csrf.disable())

                // Stateless authentication (JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth

                        // Allow CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // OpenAPI documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Login is the only public authentication endpoint
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()

                        // Invitation validation and account setup are public token flows
                        .requestMatchers(HttpMethod.POST,
                                "/employee-invitations/validate",
                                "/employee-invitations/accept"
                        ).permitAll()

                        // Registration decisions are restricted to administrators
                        .requestMatchers(HttpMethod.POST,
                                "/registration-requests/*/approve",
                                "/registration-requests/*/reject"
                        ).hasRole("ADMIN")

                        // Public registration requests do not create user accounts
                        .requestMatchers(HttpMethod.POST, "/registration-requests").permitAll()

                        // Pending registration requests are reviewed by administrators
                        .requestMatchers(HttpMethod.GET, "/registration-requests/pending")
                        .hasRole("ADMIN")

                        // Employee login accounts are created by administrators only
                        .requestMatchers(HttpMethod.POST, "/users/employee-account")
                        .hasRole("ADMIN")

                        // Administrative dashboard statistics
                        .requestMatchers(HttpMethod.GET, "/dashboard/admin-summary")
                        .hasRole("ADMIN")

                        // Tasks are assigned by administrators only
                        .requestMatchers(HttpMethod.POST, "/tasks")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/tasks/*/request-changes")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/tasks/*/activities")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.GET, "/tasks", "/tasks/*")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.POST,
                                "/tasks/*/accept", "/tasks/*/decline")
                        .hasRole("EMPLOYEE")

                        .requestMatchers(HttpMethod.PATCH, "/tasks/*/progress")
                        .hasRole("EMPLOYEE")

                        .requestMatchers(HttpMethod.PATCH, "/tasks/*/checklist/*")
                        .hasRole("EMPLOYEE")

                        .requestMatchers(HttpMethod.PATCH, "/tasks/*/archive")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/tasks/*")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/tasks/*/attachments")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.GET,
                                "/tasks/*/attachments", "/tasks/*/attachments/*/download")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.GET, "/notifications")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.PATCH, "/notifications/*/read")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        // Work-hours reports are available only to administrators
                        .requestMatchers(HttpMethod.GET, "/work-shifts/report", "/work-shifts/report/**")
                        .hasRole("ADMIN")

                        // Current shift status belongs to the linked employee
                        .requestMatchers(HttpMethod.GET, "/work-shifts/current")
                        .hasRole("EMPLOYEE")

                        // Work shifts are managed by the linked employee only
                        .requestMatchers(HttpMethod.POST, "/work-shifts/**")
                        .hasRole("EMPLOYEE")

                        // Department permissions
                        .requestMatchers(HttpMethod.GET, "/departments/**")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.POST, "/departments/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/departments/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/departments/**")
                        .hasRole("ADMIN")
                        // Employee permissions
                        .requestMatchers(HttpMethod.GET, "/employees/**")
                        .hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(HttpMethod.POST, "/employees/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/employees/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/employees/**")
                        .hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Execute JWT filter before Spring Security authentication
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // React development server
        configuration.setAllowedOrigins(
                List.of(allowedOrigin)
        );

        // Allowed HTTP methods
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allow all request headers
        configuration.setAllowedHeaders(
                List.of("*")
        );

        // Allow Authorization header
        configuration.setExposedHeaders(
                List.of("Authorization")
        );

        // JWT does not require cookies,
        // but enabling credentials makes future extensions easier.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


