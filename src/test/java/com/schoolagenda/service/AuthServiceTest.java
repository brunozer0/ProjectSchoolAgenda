package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.auth.LoginRequest;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.UserRepository;
import com.schoolagenda.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepository;
    @Mock JwtTokenProvider tokenProvider;
    @Mock AuthenticationManager authManager;
    @InjectMocks AuthService service;

    private LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setEmail("professor@escola.com");
        request.setPassword("senha-teste");
        return request;
    }

    @Test
    void authenticatedActiveUserReceivesTokenAndProfile() {
        User user = new User();
        user.setName("Professor");
        user.setRole(new Role(RoleName.TEACHER));
        when(userRepository.findByEmailAndDeletedAtIsNull("professor@escola.com"))
                .thenReturn(Optional.of(user));
        when(tokenProvider.generate(user)).thenReturn("token-teste");
        var result = service.login(request());
        assertEquals("token-teste", result.getToken());
        assertEquals("Professor", result.getName());
        assertEquals("TEACHER", result.getRole());
        var credentials = org.mockito.ArgumentCaptor.forClass(Authentication.class);
        verify(authManager).authenticate(credentials.capture());
        assertEquals("professor@escola.com", credentials.getValue().getPrincipal());
        assertEquals("senha-teste", credentials.getValue().getCredentials());
        var order = inOrder(authManager, userRepository, tokenProvider);
        order.verify(authManager).authenticate(any());
        order.verify(userRepository).findByEmailAndDeletedAtIsNull("professor@escola.com");
        order.verify(tokenProvider).generate(user);
    }

    @Test
    void invalidCredentialsNeverGenerateToken() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));
        assertThrows(BadCredentialsException.class, () -> service.login(request()));
        verifyNoInteractions(userRepository, tokenProvider);
    }

    @Test
    void missingOrDeletedUserNeverReceivesToken() {
        assertThrows(ResourceNotFoundException.class, () -> service.login(request()));
        verifyNoInteractions(tokenProvider);
    }
}
