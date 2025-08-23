package com.enterprise.user_service.service;

import com.enterprise.common.exception.BusinessException;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.user_service.dto.CreateUserRequest;
import com.enterprise.user_service.dto.UpdateUserRequest;
import com.enterprise.user_service.dto.UserDto;
import com.enterprise.user_service.entity.User;
import com.enterprise.user_service.mapper.UserMapper;
import com.enterprise.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public UserDto createUser(CreateUserRequest request) {
        log.debug("Creating user with username: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USER_EXISTS", "Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "Email already exists");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(User.UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .build();
        
        // Set default role if not provided
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            user.setRoles(Set.of(User.Role.USER));
        } else {
            Set<User.Role> roles = request.getRoles().stream()
                    .map(User.Role::valueOf)
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        
        User savedUser = userRepository.save(user);
        
        // Publish user created event
        publishUserEvent("user.created", savedUser);
        
        return userMapper.toDto(savedUser);
    }
    
    @Override
    @Cacheable(value = "users", key = "#id")
    public UserDto getUserById(String id) {
        log.debug("Fetching user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toDto(user);
    }
    
    @Override
    @Cacheable(value = "users", key = "#username")
    public UserDto getUserByUsername(String username) {
        log.debug("Fetching user with username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return userMapper.toDto(user);
    }
    
    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination");
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#id")
    public UserDto updateUser(String id, UpdateUserRequest request) {
        log.debug("Updating user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        // Update only non-null fields
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new BusinessException("EMAIL_EXISTS", "Email already exists");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }
        
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPhoneVerified(false);
        }
        
        User updatedUser = userRepository.save(user);
        
        // Publish user updated event
        publishUserEvent("user.updated", updatedUser);
        
        return userMapper.toDto(updatedUser);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(String id) {
        log.debug("Deleting user with ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        userRepository.delete(user);
        
        // Publish user deleted event
        publishUserEvent("user.deleted", user);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#id")
    public UserDto updateUserStatus(String id, String status) {
        log.debug("Updating status for user ID: {} to {}", id, status);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        try {
            User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
            user.setStatus(userStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Invalid user status: " + status);
        }
        
        User updatedUser = userRepository.save(user);
        
        // Publish user status changed event
        publishUserEvent("user.status.changed", updatedUser);
        
        return userMapper.toDto(updatedUser);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#id")
    public UserDto verifyEmail(String id) {
        log.debug("Verifying email for user ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        user.setEmailVerified(true);
        
        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        
        User updatedUser = userRepository.save(user);
        
        // Publish email verified event
        publishUserEvent("user.email.verified", updatedUser);
        
        return userMapper.toDto(updatedUser);
    }
    
    private void publishUserEvent(String topic, User user) {
        try {
            kafkaTemplate.send(topic, user.getId(), userMapper.toDto(user));
            log.info("Published event to topic: {} for user: {}", topic, user.getId());
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {} for user: {}", topic, user.getId(), e);
        }
    }
}