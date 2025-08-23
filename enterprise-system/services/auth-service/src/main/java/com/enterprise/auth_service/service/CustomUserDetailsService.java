package com.enterprise.auth_service.service;

import com.enterprise.auth_service.entity.User;
import com.enterprise.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsernameAndIsEnabledTrue(username)
            .or(() -> userRepository.findByEmailAndIsEnabledTrue(username))
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new CustomUserPrincipal(user);
    }

    /**
     * Custom UserDetails implementation
     */
    public static class CustomUserPrincipal implements UserDetails {
        
        private final User user;

        public CustomUserPrincipal(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toSet());
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return user.getIsAccountNonExpired();
        }

        @Override
        public boolean isAccountNonLocked() {
            return user.getIsAccountNonLocked();
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return user.getIsCredentialsNonExpired();
        }

        @Override
        public boolean isEnabled() {
            return user.getIsEnabled();
        }

        // Additional getters for custom fields
        public Long getUserId() {
            return user.getUserId();
        }

        public String getEmail() {
            return user.getEmail();
        }

        public String getFirstName() {
            return user.getFirstName();
        }

        public String getLastName() {
            return user.getLastName();
        }

        public String getOrganizationId() {
            return user.getOrganizationId();
        }

        public String getDepartmentId() {
            return user.getDepartmentId();
        }

        public boolean isMfaEnabled() {
            return user.getIsMfaEnabled();
        }

        public User getUser() {
            return user;
        }
    }
}