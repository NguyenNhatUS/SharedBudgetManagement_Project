package com.nhat.SharedBudgetManagement.service;

import com.nhat.SharedBudgetManagement.entity.User;


public interface UserService {

    User getUserById(Long userId);

    User getUserByEmail(String email);

    boolean existsByEmail(String email);

    User updateProfile(Long userId, String fullName);
}
