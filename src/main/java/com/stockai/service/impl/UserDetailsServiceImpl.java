package com.stockai.service.impl;

import com.stockai.dto.PasswordResetDto;
import com.stockai.dto.UserDetailsDto;
import com.stockai.entity.AuditLogEntity;
import com.stockai.entity.PasswordResetOtpEntity;
import com.stockai.entity.PlanEntity;
import com.stockai.entity.User;
import com.stockai.repository.AuditLogRepository;
import com.stockai.repository.PasswordResetOtpRepository;
import com.stockai.repository.UserRepository;
import com.stockai.repository.PlansRepository;
import com.stockai.service.interf.EmailDeliveryService;
import com.stockai.service.interf.UserDetailsService;
import com.stockai.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int OTP_MAX_PER_HOUR = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlansRepository plansRepository;

    @Autowired
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Autowired
    private AuditLogRepository authAuditLogRepository;

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public UserDetailsDto login(UserDetailsDto userDetailsDto) {
        if (userDetailsDto == null || userDetailsDto.getEmail() == null || userDetailsDto.getPassword() == null) {
            audit(null, userDetailsDto == null ? null : userDetailsDto.getEmail(), "Login_Failure");
            throw new BadCredentialsException("Invalid email or password");
        }
        String email = userDetailsDto.getEmail().trim().toLowerCase();
        log.info("Login request received");
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            audit(null, email, "Login_Failure");
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!passwordEncoder.matches(userDetailsDto.getPassword(), user.getPassword())) {
            if (userDetailsDto.getPassword().equals(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(userDetailsDto.getPassword()));
                userRepository.save(user);
            } else {
                audit(user, email, "Login_Failure");
                throw new BadCredentialsException("Invalid email or password");
            }
        }

        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles().stream().map(role -> role.getRoleName()).distinct().toList();
        UserDetailsDto response = new UserDetailsDto();
        response.setEmail(user.getEmail());
        response.setUserId(user.getId().toString());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoles(roles);
        response.setPhoneNumber(String.valueOf(user.getMobileNumber()));
        response.setImage(user.getImage());
        response.setAddress(user.getAddress());
        // Verify the user has an active plan before issuing a token
        PlanEntity plan = plansRepository.findByUserName(user.getEmail());
        java.time.LocalDate today = java.time.LocalDate.now();
        boolean hasActivePlan = false;
        if (plan != null) {
            // plan.status expected to indicate active state (e.g., "ACTIVE").
            boolean statusActive = plan.getStatus() != null && plan.getStatus().toLowerCase().contains("active");
            boolean withinRange = (plan.getFromDate() == null || !plan.getFromDate().isAfter(today))
                    && (plan.getToDate() == null || !plan.getToDate().isBefore(today));
            hasActivePlan = statusActive && withinRange;
        }
        if (!hasActivePlan) {
            audit(user, email, "Login_Failure_No_Active_Plan");
            throw new IllegalStateException("User doesnot have active plan");
        }

        response.setToken(jwtTokenUtil.generateToken(user.getEmail(), user.getId().toString(), roles));
        audit(user, email, "Login_Success");
        return response;
    }

    @Override
    @Transactional
    public Boolean forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            audit(null, normalizedEmail, "Password_Reset_Unknown_Email");
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (passwordResetOtpRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusHours(1))
                >= OTP_MAX_PER_HOUR) {
            return false;
        }
        PasswordResetOtpEntity latestOtp = passwordResetOtpRepository
                .findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latestOtp != null && latestOtp.getCreatedAt().isAfter(now.minusSeconds(OTP_RESEND_COOLDOWN_SECONDS))) {
            return false;
        }

        String otp = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        PasswordResetOtpEntity otpEntity = new PasswordResetOtpEntity();
        otpEntity.setUserId(user.getId());
        otpEntity.setEmail(normalizedEmail);
        otpEntity.setOtpHash(passwordEncoder.encode(otp));
        otpEntity.setCreatedAt(now);
        otpEntity.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        otpEntity.setFailedAttempts(0);
        passwordResetOtpRepository.save(otpEntity);

        emailDeliveryService.sendPasswordResetOtp(normalizedEmail, otp);
        audit(user, normalizedEmail, "Password_Reset_Otp_Sent");
        return true;
    }

    @Override
    @Transactional
    public boolean resetPassword(PasswordResetDto request) {
        if (request == null || request.getEmail() == null || request.getOtp() == null
                || request.getNewPassword() == null || request.getConfirmPassword() == null
                || request.getNewPassword().length() < 8
                || !request.getNewPassword().equals(request.getConfirmPassword())) {
            return false;
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return false;
        }

        PasswordResetOtpEntity otp = passwordResetOtpRepository
                .findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (otp == null || otp.getConsumedAt() != null || !otp.getExpiresAt().isAfter(now)
                || otp.getFailedAttempts() >= OTP_MAX_ATTEMPTS) {
            return false;
        }
        if (!passwordEncoder.matches(request.getOtp(), otp.getOtpHash())) {
            otp.setFailedAttempts(otp.getFailedAttempts() + 1);
            if (otp.getFailedAttempts() >= OTP_MAX_ATTEMPTS) {
                otp.setConsumedAt(now);
            }
            passwordResetOtpRepository.save(otp);
            audit(user, email, "Password_Reset_Otp_Failure");
            return false;
        }

        otp.setConsumedAt(now);
        passwordResetOtpRepository.save(otp);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        audit(user, email, "Password_Updated");
        return true;
    }

    private void audit(User user, String email, String eventType) {
        AuditLogEntity entry = new AuditLogEntity();
        entry.setUserId(user == null ? null : user.getId());
        entry.setEmail(email);
        entry.setEvent_type(eventType);
        entry.setEventAt(LocalDateTime.now());
        entry.setCreatedDate(user.getCreatedDate());
        entry.setUpdatedDate(user.getUpdatedDate());
        authAuditLogRepository.saveAuditLogEntity(entry);
    }
}