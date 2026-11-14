package com.stockai.service.interf;

import com.stockai.dto.PasswordResetDto;
import com.stockai.dto.UserDetailsDto;

public interface UserDetailsService {

    UserDetailsDto login(UserDetailsDto userDetailsDto);

    Boolean forgotPassword(String email);

    boolean resetPassword(PasswordResetDto request);

}
