package com.philippos.employeemanagement.security;

import com.philippos.employeemanagement.dto.response.ApiErrorResponse;
import com.philippos.employeemanagement.entity.EmployeeStatus;
import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import com.philippos.employeemanagement.service.JwtService;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Get the Authorization header from the HTTP request
        String authorizationHeader =
                request.getHeader("Authorization");

        // If there is no Bearer token, continue to the next filter
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Remove "Bearer " and keep only the JWT
        String token = authorizationHeader.substring(7);

        try {

            // Extract the username stored inside the JWT
            String username = jwtService.extractUsername(token);

            // Authenticate only if the token is valid
            // and the user is not already authenticated
            if (jwtService.isTokenValid(token) &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                // Find the user in the database
                User user = userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

                if (user.getRole() == Role.EMPLOYEE) {
                    if (user.getEmployee() == null) {
                        throw new IllegalArgumentException("Employee account is unavailable");
                    }
                    if (user.getEmployee().getStatus() == EmployeeStatus.INACTIVE) {
                        throw new IllegalArgumentException("Employee account is inactive");
                    }
                }

                // Convert our role (ADMIN, EMPLOYEE)
                // into a Spring Security authority
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        );

                // Create an Authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                List.of(authority)
                        );

                // Tell Spring Security that this request
                // belongs to an authenticated user
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException exception) {

            // Invalid, expired or malformed JWT
            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Invalid or expired token"
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    objectMapper.writeValueAsString(errorResponse)
            );
            return;
        }

        // Continue to the next filter and eventually the Controller
        filterChain.doFilter(request, response);
    }
}
