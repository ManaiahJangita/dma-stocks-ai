package com.stockai.service.interf;

import com.stockai.dto.UserDetailsDto;
import com.stockai.dto.PasswordResetDto;

public interface UserDetailsService {

    UserDetailsDto login(UserDetailsDto userDetailsDto);

    public Boolean forgotPassword(String email);

    boolean resetPassword(PasswordResetDto request);

}
