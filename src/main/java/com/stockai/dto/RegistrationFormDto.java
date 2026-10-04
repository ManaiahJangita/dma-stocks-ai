package com.stockai.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegistrationFormDto {
    private String firstName;
    private String lastName;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;
    private long mobileNumber;
    private String email;
    private String address;
    private String password;
    private List<String> roles;
    private MultipartFile image;
}
