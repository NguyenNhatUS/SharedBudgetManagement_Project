package com.nhat.SharedBudgetManagement.service.impl;

import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.exception.ErrorCode;
import com.nhat.SharedBudgetManagement.exception.ResourceNotFoundException;
import com.nhat.SharedBudgetManagement.repository.UserRepository;
import com.nhat.SharedBudgetManagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String fullName, String avatarUrl) {
        User user = getUserById(userId);
        user.setFullName(fullName);
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }
}
