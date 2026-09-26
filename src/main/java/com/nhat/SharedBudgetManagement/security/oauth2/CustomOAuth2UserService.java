package com.nhat.SharedBudgetManagement.security.oauth2;

import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.exception.BadRequestException;
import com.nhat.SharedBudgetManagement.exception.ErrorCode;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String pictureUrl = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub");

        if (email == null || email.isBlank()) {
            throw new BadRequestException(
                    ErrorCode.OAUTH2_EMAIL_NOT_PROVIDED,
                    "Email not provided by Google. Please allow email access."
            );
        }

        email = email.toLowerCase().trim();

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Cập nhật thông tin từ Google nếu cần
            boolean updated = false;
            if (name != null && !name.equals(user.getFullName())) {
                user.setFullName(name);
                updated = true;
            }
            if (pictureUrl != null && !pictureUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(pictureUrl);
                updated = true;
            }
            if (user.getProviderId() == null && providerId != null) {
                user.setProviderId(providerId);
                user.setAuthProvider(AuthProvider.GOOGLE);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
                log.info("Updated existing user from Google profile: {}", user.getEmail());
            }
        } else {
            // Tạo user mới từ Google profile
            user = User.builder()
                    .email(email)
                    .fullName(name != null ? name : email)
                    .avatarUrl(pictureUrl)
                    .password("N/A")
                    .role(UserRole.ROLE_USER)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .build();
            user = userRepository.save(user);
            log.info("Created new user from Google OAuth2: {}", user.getEmail());
        }

        return UserPrincipal.create(user, attributes);
    }
}
