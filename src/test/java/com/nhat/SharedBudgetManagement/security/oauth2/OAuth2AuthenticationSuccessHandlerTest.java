package com.nhat.SharedBudgetManagement.security.oauth2;

import com.nhat.SharedBudgetManagement.entity.RefreshToken;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.repository.RefreshTokenRepository;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.security.JwtTokenProvider;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RedirectStrategy redirectStrategy;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    private User user;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(successHandler, "authorizedRedirectUri", "http://localhost:3000/oauth2/callback");
        // Inject mock RedirectStrategy so we can verify redirect calls
        successHandler.setRedirectStrategy(redirectStrategy);

        user = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .fullName("Test User")
                .password("N/A")
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-123")
                .createdAt(LocalDateTime.now())
                .build();

        principal = UserPrincipal.create(user);
    }

    @Test
    @DisplayName("Should generate JWT tokens and redirect to frontend with tokens in query params")
    void onAuthenticationSuccess_ShouldRedirectWithTokens() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("mock-access-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(response.isCommitted()).thenReturn(false);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then — verify redirect URL contains tokens
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

        String redirectUrl = urlCaptor.getValue();
        assertThat(redirectUrl).startsWith("http://localhost:3000/oauth2/callback");
        assertThat(redirectUrl).contains("token=mock-access-token");
        assertThat(redirectUrl).contains("refreshToken=mock-refresh-token");
    }

    @Test
    @DisplayName("Should save refresh token to database")
    void onAuthenticationSuccess_ShouldSaveRefreshToken() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("mock-access-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(response.isCommitted()).thenReturn(false);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then — verify refresh token was saved to DB
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("mock-refresh-token");
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getRevoked()).isFalse();
        assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should not redirect when response is already committed")
    void onAuthenticationSuccess_ResponseCommitted_ShouldNotRedirect() throws Exception {
        // Given
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("mock-access-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("mock-refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(response.isCommitted()).thenReturn(true);

        // When
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Then — should NOT redirect since response is committed
        verify(redirectStrategy, never()).sendRedirect(any(), any(), anyString());
    }
}
