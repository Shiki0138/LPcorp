package com.enterprise.user_service.service;

import com.enterprise.user_service.dto.CreateUserRequest;
import com.enterprise.user_service.dto.UpdateUserRequest;
import com.enterprise.user_service.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserDto createUser(CreateUserRequest request);
    UserDto getUserById(String id);
    UserDto getUserByUsername(String username);
    Page<UserDto> getAllUsers(Pageable pageable);
    UserDto updateUser(String id, UpdateUserRequest request);
    void deleteUser(String id);
    UserDto updateUserStatus(String id, String status);
    UserDto verifyEmail(String id);
}