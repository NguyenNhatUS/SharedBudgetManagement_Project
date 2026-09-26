package com.nhat.SharedBudgetManagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhat.SharedBudgetManagement.controller.v1.UserController;
import com.nhat.SharedBudgetManagement.dto.request.UpdateProfileRequest;
import com.nhat.SharedBudgetManagement.dto.response.UserResponse;
import com.nhat.SharedBudgetManagement.entity.User;
import com.nhat.SharedBudgetManagement.entity.enums.AuthProvider;
import com.nhat.SharedBudgetManagement.entity.enums.UserRole;
import com.nhat.SharedBudgetManagement.exception.GlobalExceptionHandler;
import com.nhat.SharedBudgetManagement.mapper.UserMapper;
import com.nhat.SharedBudgetManagement.security.UserPrincipal;
import com.nhat.SharedBudgetManagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for UserController")
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserResponse testUserResponse;
    private UserPrincipal testPrincipal;

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

        testUserResponse = UserResponse.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(UserRole.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .build();

        testPrincipal = UserPrincipal.create(testUser);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(UserPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return testPrincipal;
                    }
                })
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/users/me - Success (200 OK)")
    void getCurrentUser_success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Success (200 OK)")
    void getUser_success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/profile - Success (200 OK)")
    void updateProfile_success() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");
        request.setAvatarUrl("https://example.com/new.jpg");

        User updatedUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Updated")
                .avatarUrl("https://example.com/new.jpg")
                .role(UserRole.ROLE_USER)
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Updated")
                .avatarUrl("https://example.com/new.jpg")
                .role(UserRole.ROLE_USER)
                .build();

        when(userService.updateProfile(eq(1L), eq("John Updated"), eq("https://example.com/new.jpg")))
                .thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/users/1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.fullName").value("John Updated"));
    }
}
