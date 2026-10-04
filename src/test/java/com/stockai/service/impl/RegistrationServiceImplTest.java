package com.stockai.service.impl;

import com.stockai.dto.UserDto;
import com.stockai.entity.User;
import com.stockai.repository.AuditLogRepository;
import com.stockai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void registerUserStoresImage() {
        UserDto request = new UserDto();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setGender("Female");
        request.setDateOfBirth(LocalDate.of(1995, 6, 15));
        request.setMobileNumber(1234567890L);
        request.setEmail("jane@example.com");
        request.setPassword("password123");
        request.setImage("data:image/png;base64,abc123");
        request.setAddress("123 Market Street");

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        registrationService.registerUser(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("data:image/png;base64,abc123", savedUser.getValue().getImage());
        assertEquals("123 Market Street", savedUser.getValue().getAddress());
        assertEquals("Female", savedUser.getValue().getGender());
        assertEquals(LocalDate.of(1995, 6, 15), savedUser.getValue().getDateOfBirth());
    }
}
