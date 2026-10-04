package com.stockai.service.impl;

import com.stockai.dto.UserDetailsDto;
import com.stockai.entity.User;
import com.stockai.repository.AuditLogRepository;
import com.stockai.repository.PasswordResetOtpRepository;
import com.stockai.repository.UserRepository;
import com.stockai.utils.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loginReturnsStoredImage() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPassword("encoded-password");
        user.setMobileNumber(1234567890L);
        user.setImage("data:image/png;base64,abc123");
        user.setAddress("123 Market Street");
        user.setCreatedDate(LocalDateTime.now());
        user.setUpdatedDate(LocalDate.now());

        UserDetailsDto request = new UserDetailsDto();
        request.setEmail("jane@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenUtil.generateToken(user.getEmail(), user.getId().toString(), java.util.List.of()))
                .thenReturn("token");

        UserDetailsDto response = userDetailsService.login(request);

        assertEquals("Jane", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("data:image/png;base64,abc123", response.getImage());
        assertEquals("123 Market Street", response.getAddress());
        assertEquals("token", response.getToken());
    }
}
