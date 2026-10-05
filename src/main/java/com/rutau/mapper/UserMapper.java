package com.rutau.mapper;

import com.rutau.dto.response.UserResponseDTO;
import com.rutau.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toResponse(User user);
}