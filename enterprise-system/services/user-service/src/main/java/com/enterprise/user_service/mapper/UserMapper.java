package com.enterprise.user_service.mapper;

import com.enterprise.user_service.dto.UserDto;
import com.enterprise.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    UserDto toDto(User user);
    
    User toEntity(UserDto dto);
    
    default java.util.Set<String> mapRoles(User user) {
        if (user.getRoles() == null) {
            return null;
        }
        return user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}