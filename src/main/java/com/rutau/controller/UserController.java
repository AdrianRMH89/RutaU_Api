package com.rutau.controller;

import com.rutau.dto.response.UserResponseDTO;
import com.rutau.mapper.UserMapper;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUser currentUser;
    private final UserMapper userMapper;

    // Devuelve los datos del usuario logueado (requiere token)
    @GetMapping("/me")
    public UserResponseDTO me() {
        return userMapper.toResponse(currentUser.get());
    }
}