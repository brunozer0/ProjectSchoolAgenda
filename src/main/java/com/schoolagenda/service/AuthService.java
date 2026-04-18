package com.schoolagenda.service;


import com.schoolagenda.domain.entity.User;
import com.schoolagenda.dto.auth.LoginRequest;
import com.schoolagenda.dto.auth.LoginResponse;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.UserRepository;
import com.schoolagenda.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authManager;

    public LoginResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        String token = tokenProvider.generate(user);

        return new LoginResponse(
                token,
                user.getName(),
                user.getRole().getName().name()
        );
    }
}
