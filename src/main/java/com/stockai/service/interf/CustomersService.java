package com.stockai.service.interf;

import com.stockai.dto.CustomerDto;

import java.util.List;

public interface CustomersService {

    List<CustomerDto> getCustomersFilters(CustomerDto filters);

    List<CustomerDto> fetchAllCustomersInfo();

    int syncAllCustomersInfo();
}
