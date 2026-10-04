package com.stockai.service.interf;

public interface EmailDeliveryService {
    void sendPasswordResetOtp(String email, String otp);
}