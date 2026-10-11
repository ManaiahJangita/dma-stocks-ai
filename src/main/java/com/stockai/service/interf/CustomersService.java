package com.stockai.service.interf;

import com.stockai.dto.CustomerDto;
import com.stockai.dto.CustomersFilterDto;

import java.util.List;

public interface CustomersService {

    List<CustomerDto> getCustomersFilters(CustomersFilterDto filters);

    List<CustomerDto> fetchAllCustomersInfo();

    int syncAllCustomersInfo();
}
