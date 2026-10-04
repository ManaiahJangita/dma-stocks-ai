package com.stockai.utils;

import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

/**
 * This class is used to generate OTP code.
 *
 * @author Sudhakar.Badugu
 * 05-03-2023
 */
public class OtpUtil {
    private OtpUtil() {
    }

    /**
     * Genrate time based OTP code.
     *
     * @param secretKey
     * @return
     */
    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }
}