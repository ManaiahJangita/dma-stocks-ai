package com.stockai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordResetDto {
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String otp;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String newPassword;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String confirmPassword;
}