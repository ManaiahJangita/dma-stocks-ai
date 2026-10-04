package com.stockai.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class EncryptUtil {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String SECRET = "dmastockaler##%%";

    private static final String CHARSET = "UTF-8";

    private static String adjustKey(String key) {
        int length = 16; // AES requires 16, 24, or 32 bytes
        if (key.length() < length) {
            return String.format("%-" + length + "s", key).substring(0, length); // Pad with spaces
        } else {
            return key.substring(0, length); // Trim to 16 bytes
        }
    }

    public static String encrypt(String data) throws Exception {
        String adjustedKey = adjustKey(SECRET);
        SecretKeySpec secretKeySpec = new SecretKeySpec(adjustedKey.getBytes(CHARSET), "AES");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

        byte[] encryptedData = cipher.doFinal(data.getBytes(CHARSET));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static String decryptThirdParty(String strToDecrypt) {
        try{
            return decrypt(strToDecrypt);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String decrypt(String encryptedData) throws Exception {
        String adjustedKey = adjustKey(SECRET);
        SecretKeySpec secretKeySpec = new SecretKeySpec(adjustedKey.getBytes(CHARSET), "AES");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodedData);

        return new String(decryptedData, CHARSET);
    }
}