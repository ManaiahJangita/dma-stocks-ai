package com.stockai.repository;

import com.stockai.dto.CustomersFilterDto;
import com.stockai.entity.CustomerEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomersRespository {

     List<CustomerEntity> fetchAllCustomers();

    List<CustomerEntity> findCustomers(CustomersFilterDto filters);

    int saveAllCustomers(List<CustomerEntity> entities);

}
