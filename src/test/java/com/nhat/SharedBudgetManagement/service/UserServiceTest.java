package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    @DisplayName("getUserById - Success: should return user when ID exists")
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    @DisplayName("getUserById - Not Found: should throw ResourceNotFoundException when user ID does not exist")
    void getUserById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("getUserByEmail - Success: should return user when email exists")
    void getUserByEmail_success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("user@example.com");

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmail());
        assertEquals("John Doe", result.getFullName());
    }

    @Test
    @DisplayName("getUserByEmail - Not Found: should throw ResourceNotFoundException when email does not exist")
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("nonexistent@example.com"));
    }

    @Test
    @DisplayName("existsByEmail - Should return true when email exists and false when not")
    void existsByEmail_check() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("other@example.com")).thenReturn(false);

        assertTrue(userService.existsByEmail("user@example.com"));
        assertFalse(userService.existsByEmail("other@example.com"));
    }

    @Test
    @DisplayName("updateProfile - Success: should update fullName, avatarUrl and save user")
    void updateProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateProfile(1L, "Jane Doe", "https://example.com/new-avatar.jpg");

        assertNotNull(result);
        assertEquals("Jane Doe", testUser.getFullName());
        assertEquals("https://example.com/new-avatar.jpg", testUser.getAvatarUrl());
        verify(userRepository).save(testUser);
    }
}
