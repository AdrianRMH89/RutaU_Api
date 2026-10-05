package com.rutau.controller;

import com.rutau.dto.response.PublicProfileResponseDTO;
import com.rutau.dto.response.UserResponseDTO;
import com.rutau.mapper.UserMapper;
import com.rutau.security.CurrentUser;
import com.rutau.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUser currentUser;
    private final UserMapper userMapper;
    private final UserService userService;

    // Devuelve los datos del usuario logueado (requiere token)
    @GetMapping("/me")
    public UserResponseDTO me() {
        return userMapper.toResponse(currentUser.get());
    }

    // US10 - Perfil público de un conductor (nombre, universidad, promedio y viajes realizados)
    @GetMapping("/{id}/profile")
    public PublicProfileResponseDTO publicProfile(@PathVariable Long id) {
        return userService.getPublicProfile(id);
    }
}