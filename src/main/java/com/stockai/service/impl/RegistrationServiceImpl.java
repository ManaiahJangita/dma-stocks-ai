package com.stockai.service.impl;

import com.stockai.dto.UserDto;
import com.stockai.entity.AuditLogEntity;
import com.stockai.entity.User;
import com.stockai.entity.UserRoles;
import com.stockai.enums.Roles;
import com.stockai.repository.AuditLogRepository;
import com.stockai.repository.UserRepository;
import com.stockai.service.interf.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public String registerUser(UserDto userDetailsDto) {
        if (userDetailsDto == null || userDetailsDto.getFirstName() == null || userDetailsDto.getFirstName().isBlank()
            || userDetailsDto.getLastName() == null || userDetailsDto.getLastName().isBlank()
            || userDetailsDto.getEmail() == null || userDetailsDto.getEmail().isBlank()
                || userDetailsDto.getPassword() == null || userDetailsDto.getPassword().length() < 8
                || userDetailsDto.getMobileNumber() <= 0) {
            throw new IllegalArgumentException("Valid email, password (at least 8 characters), and mobile number are required");
        }

        String email = userDetailsDto.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()
                || userRepository.existsByMobileNumber(userDetailsDto.getMobileNumber())) {
            throw new IllegalArgumentException("Email or mobile number is already registered");
        }

        User userModel = new User();
        userModel.setFirstName(userDetailsDto.getFirstName().trim());
        userModel.setLastName(userDetailsDto.getLastName().trim());
        userModel.setGender(userDetailsDto.getGender());
        userModel.setDateOfBirth(userDetailsDto.getDateOfBirth());
        userModel.setMobileNumber(userDetailsDto.getMobileNumber());
        userModel.setEmail(email);
        userModel.setAddress(userDetailsDto.getAddress());
        userModel.setPassword(passwordEncoder.encode(userDetailsDto.getPassword()));
        userModel.setImage(userDetailsDto.getImage());
        Set<String> requestedRoles = userDetailsDto.getRoles() == null ? Set.of() : new LinkedHashSet<>(userDetailsDto.getRoles());
        if (requestedRoles.isEmpty()) {
            requestedRoles = Set.of(Roles.USER.getName());
        }

        List<UserRoles> userRoles = requestedRoles.stream()
                .map(String::trim)
                .map(Roles::getType)
                .map(role -> {
                    if (role == null) {
                        throw new IllegalArgumentException("Unsupported role");
                    }
                    if (role == Roles.ADMIN) {
                        throw new IllegalArgumentException("ADMIN role cannot be assigned during public registration");
                    }
                    UserRoles userRole = new UserRoles();
                    userRole.setRoleName(role.getName());
                    userRole.setUser(userModel);
                    return userRole;
                }).toList();
        userModel.setRoles(userRoles);
        userModel.setCreatedDate(LocalDateTime.now());
        userModel.setUpdatedDate(LocalDate.now());
        userModel.setStatus("Active");
        userRepository.save(userModel);
        audit(userModel,userModel.getEmail(),"User_Register_Success_fully");
        return "User registered successfully";
    }

    private void audit(User user, String email, String eventType) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setUserId(user == null ? null : user.getId());
        entry.setEmail(email);
        entry.setEvent_type(eventType);
        entry.setEventAt(LocalDateTime.now());
        entry.setCreatedDate(user.getCreatedDate());
        entry.setUpdatedDate(user.getUpdatedDate());
        auditLogRepository.saveAuditLogEntity(entry);
    }
}