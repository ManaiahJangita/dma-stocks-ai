package com.stockai.controller;

import com.stockai.apiconstants.ApiConstants;
import com.stockai.dto.RegistrationFormDto;
import com.stockai.dto.UserDetailsDto;
import com.stockai.dto.PasswordResetDto;
import com.stockai.dto.UserDto;
import com.stockai.service.interf.RegistrationService;
import com.stockai.service.interf.UserDetailsService;
import com.stockai.utils.AuthUtils;
import com.stockai.utils.CookieUtils;
import com.stockai.utils.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserDetailsService loginService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;


    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> registerMultipart(@ModelAttribute RegistrationFormDto request) {
        UserDto user = new UserDto();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setMobileNumber(request.getMobileNumber());
        user.setEmail(request.getEmail());
        user.setAddress(request.getAddress());
        user.setPassword(request.getPassword());
        user.setRoles(request.getRoles());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                byte[] imageBytes = request.getImage().getBytes();
                String contentType = detectImageContentType(imageBytes);
                if (contentType == null) {
                    return ResponseEntity.badRequest().body("Uploaded file must be a supported image");
                }
                user.setImage("data:" + contentType + ";base64,"
                        + Base64.getEncoder().encodeToString(imageBytes));
            } catch (IOException exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Unable to read uploaded image");
            }
        }
        return registerUser(user);
    }

    private String detectImageContentType(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E
                && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A
                && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a') {
            return "image/gif";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E'
                && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M') {
            return "image/bmp";
        }
        return null;
    }

    private ResponseEntity<String> registerUser(UserDto request) {
        try {
            String response = registrationService.registerUser(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDetailsDto request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        try {
            UserDetailsDto response = loginService.login(request);
            CookieUtils.addCookie(ApiConstants.USER_ID_COOKIE_NAME, response.getToken(), jwtTokenUtil.getCookieMaxAgeSeconds(), servletResponse, servletRequest.isSecure());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        } catch (IllegalStateException exception) {
            // Raised when user does not have an active plan
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout User", description = "Logout User")
    public ResponseEntity<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.removeCookie(ApiConstants.USER_ID_COOKIE_NAME, response, request.isSecure());
        AuthUtils.removeAuthentication();
        Boolean status = true;
        return new ResponseEntity<>(status, HttpStatus.OK);
    }

    @PostMapping("/forgot/{email}")
    @Operation(summary = "Forgot Password", description = "Forgot Password")
    public ResponseEntity<Boolean> forgotPassword(@PathVariable String email) {
        try {
            return ResponseEntity.ok(loginService.forgotPassword(email));
        } catch (IllegalStateException | MailException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(false);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Boolean> resetPassword(@RequestBody PasswordResetDto request) {
        return ResponseEntity.ok(loginService.resetPassword(request));
    }
}
