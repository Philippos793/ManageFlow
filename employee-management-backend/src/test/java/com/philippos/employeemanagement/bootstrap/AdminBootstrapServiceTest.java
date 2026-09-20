package com.philippos.employeemanagement.bootstrap;

import com.philippos.employeemanagement.entity.Role;
import com.philippos.employeemanagement.entity.User;
import com.philippos.employeemanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AdminBootstrapService(userRepository, passwordEncoder);
    }

    @Test
    void createsInitialAdminWhenNoAdminExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("initial.admin")).thenReturn(false);

        service.createInitialAdminIfRequired(" Initial.Admin ", "SecurePassword123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertEquals("initial.admin", saved.getUsername());
        assertEquals(Role.ADMIN, saved.getRole());
        assertNull(saved.getEmployee());
    }

    @Test
    void existingAdminPreventsAnotherCreation() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        service.createInitialAdminIfRequired(null, null);

        verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verify(userRepository, never()).existsByUsernameIgnoreCase(
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void missingVariablesFailWhenNoAdminExists() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createInitialAdminIfRequired(null, null));

        assertTrue(exception.getMessage().contains("ADMIN_USERNAME"));
        assertFalse(exception.getMessage().contains("SecurePassword123"));
    }

    @Test
    void duplicateUsernameFailsBootstrap() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("existing.user")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.createInitialAdminIfRequired(
                        "existing.user", "SecurePassword123"));

        verify(userRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void storesPasswordAsBcryptHash() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);

        service.createInitialAdminIfRequired("admin", "SecurePassword123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        String storedPassword = captor.getValue().getPassword();
        assertFalse(storedPassword.equals("SecurePassword123"));
        assertTrue(passwordEncoder.matches("SecurePassword123", storedPassword));
    }
}
