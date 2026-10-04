package com.stockai.repository;

import com.stockai.entity.CustomerEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomersRespository {

     List<CustomerEntity> fetchAllCustomers();

    List<CustomerEntity> findCustomers(CustomerEntity filters);

    int saveAllCustomers(List<CustomerEntity> entities);

}
