package com.rutau.service;

import com.rutau.dto.request.LoginRequestDTO;
import com.rutau.dto.request.RegisterRequestDTO;
import com.rutau.dto.response.AuthResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.mapper.UserMapper;
import com.rutau.model.Role;
import com.rutau.model.User;
import com.rutau.repository.UserRepository;
import com.rutau.security.JwtService;
import com.rutau.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Los correos universitarios en Perú terminan en ".edu.pe" (upc.edu.pe, utp.edu.pe, etc.)
    private static final String INSTITUTIONAL_DOMAIN = ".edu.pe";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO dto) {
        String email = dto.email().trim().toLowerCase();

        // US01 - Escenario alternativo: solo correos institucionales
        if (!email.endsWith(INSTITUTIONAL_DOMAIN)) {
            throw new BusinessRuleException("Debes registrarte con tu correo institucional");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (userRepository.existsByName(dto.name().trim())) {
            throw new ConflictException("El nombre de usuario ya está en uso");
        }

        User user = new User();
        user.setName(dto.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.password()));   // se guarda cifrada
        user.setFullName(dto.fullName().trim());
        user.setUniversity(dto.university().trim());
        user.setDistrict(dto.district());
        user.setPhone(dto.phone());
        user.setEnabled(true);
        user.setRole(Role.USER);   // rol dual: el mismo usuario es conductor o pasajero
        userRepository.save(user);

        String token = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponseDTO(token, "Bearer", userMapper.toResponse(user));
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        // Si el correo o la contraseña son incorrectos, lanza BadCredentialsException (401)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email().trim().toLowerCase(), dto.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new AuthResponseDTO(token, "Bearer", userMapper.toResponse(principal.getUser()));
    }
}