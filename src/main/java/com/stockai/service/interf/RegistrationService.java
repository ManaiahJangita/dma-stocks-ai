package com.stockai.service.interf;

import com.stockai.dto.UserDetailsDto;
import com.stockai.dto.UserDto;

public interface RegistrationService {

    public String registerUser(UserDto userDetailsDto);
}
