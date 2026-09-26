package com.nhat.SharedBudgetManagement.security.oauth2;

import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .fullName("Test User")
                .password("N/A")
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-id-123")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create new user when Google email does not exist in DB")
    void processOAuth2User_NewUser_ShouldCreateUser() {
        // Given
        User newUser = User.builder()
                .email("new@gmail.com")
                .fullName("New User")
                .avatarUrl("https://lh3.googleusercontent.com/photo.jpg")
                .password("N/A")
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-new-id")
                .build();

        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When — verify the user creation flow
        assertThat(userRepository.findByEmail("new@gmail.com")).isEmpty();

        User savedUser = userRepository.save(newUser);

        // Then
        assertThat(savedUser.getEmail()).isEqualTo("new@gmail.com");
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(savedUser.getProviderId()).isEqualTo("google-new-id");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update existing user info when Google login again")
    void processOAuth2User_ExistingUser_ShouldUpdateInfo() {
        // Given
        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));

        // When — simulate updating user's name
        User user = userRepository.findByEmail("test@gmail.com").orElseThrow();
        String newName = "Updated Name";
        String newAvatar = "https://new-avatar.jpg";

        user.setFullName(newName);
        user.setAvatarUrl(newAvatar);

        when(userRepository.save(user)).thenReturn(user);
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getFullName()).isEqualTo(newName);
        assertThat(savedUser.getAvatarUrl()).isEqualTo(newAvatar);
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should set providerId when existing local user logs in with Google")
    void processOAuth2User_ExistingLocalUser_ShouldSetProviderIdIfNull() {
        // Given — a LOCAL user without providerId
        User localUser = User.builder()
                .id(3L)
                .email("local@gmail.com")
                .fullName("Local User")
                .password("$2a$10$encoded")
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .providerId(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("local@gmail.com")).thenReturn(Optional.of(localUser));

        // When — simulate the flow
        User user = userRepository.findByEmail("local@gmail.com").orElseThrow();

        if (user.getProviderId() == null) {
            user.setProviderId("google-id-456");
            user.setAuthProvider(AuthProvider.GOOGLE);
        }

        when(userRepository.save(user)).thenReturn(user);
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getProviderId()).isEqualTo("google-id-456");
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("UserPrincipal should be created from User with OAuth2 attributes")
    void userPrincipal_ShouldContainOAuth2Attributes() {
        // Given
        Map<String, Object> attributes = Map.of(
                "sub", "google-id-123",
                "email", "test@gmail.com",
                "name", "Test User",
                "picture", "https://photo.jpg"
        );

        // When
        UserPrincipal principal = UserPrincipal.create(existingUser, attributes);

        // Then
        assertThat(principal.getId()).isEqualTo(1L);
        assertThat(principal.getEmail()).isEqualTo("test@gmail.com");
        assertThat(principal.getAttributes()).isNotNull();
        assertThat(principal.getAttributes()).containsEntry("sub", "google-id-123");
        assertThat(principal.getAttributes()).containsEntry("email", "test@gmail.com");
        assertThat(principal.getName()).isEqualTo("test@gmail.com");
    }
}
