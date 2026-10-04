package com.stockai.service.impl;

import com.stockai.service.interf.EmailDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class SmtpEmailDeliveryService implements EmailDeliveryService {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailDeliveryService(
            JavaMailSender mailSender,
            @Value("${spring.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("SPRING_MAIL_FROM must be configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Stock AI password reset code");
        message.setText("Your password reset code is " + otp
                + ". It expires in 5 minutes. If you did not request this, ignore this email.");
        try{
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password reset OTP email", e);
        }
        mailSender.send(message);
        log.info("Password reset OTP sent to email: {}", email);
    }
}