package com.stockai.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private long mobileNumber;
    private String email;
    private String address;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String image;
    private List<String> roles;
}
