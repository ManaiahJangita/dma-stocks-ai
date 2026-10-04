package com.stockai.service.impl;

import com.stockai.dto.CustomerDto;
import com.stockai.dto.UserDto;
import com.stockai.entity.CustomerEntity;
import com.stockai.entity.User;
import com.stockai.repository.CustomersRespository;
import com.stockai.repository.UserRepository;
import com.stockai.service.interf.CustomersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomersServiceImpl implements CustomersService {

    @Autowired
    private CustomersRespository customersRespository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<CustomerDto> getCustomersFilters(CustomerDto filters) {
        List<CustomerDto> customers = new ArrayList<>();
        CustomerEntity entityFilters = new CustomerEntity();
        BeanUtils.copyProperties(filters, entityFilters);
        for (CustomerEntity entity : customersRespository.findCustomers(entityFilters)) {
            CustomerDto dto = new CustomerDto();
            BeanUtils.copyProperties(entity, dto);
            customers.add(dto);
        }
        return customers;
    }

    @Override
    public List<CustomerDto> fetchAllCustomersInfo() {
        List<CustomerEntity> allCustomers = customersRespository.fetchAllCustomers();
        List<CustomerDto> customerDto = new ArrayList<>();
        for(CustomerEntity entity: allCustomers){
            CustomerDto dto = new CustomerDto();
            BeanUtils.copyProperties(entity,dto);
            customerDto.add(dto);
        }
        return customerDto;
    }

    public int syncAllCustomersInfo(){
        List<User> allUsers =  userRepository.findAll();
        List<CustomerEntity> allCustomers = new ArrayList<>(allUsers.size());
        for(User user:allUsers){
            CustomerEntity dto = new CustomerEntity();
            dto.setUserId(user.getId());
            dto.setName(user.getFirstName()+" "+user.getLastName());
            dto.setPhone(String.valueOf(user.getMobileNumber()));
            dto.setEmail(user.getEmail());
            dto.setCreatedDate(user.getCreatedDate());
            dto.setUpdatedDate(user.getUpdatedDate());
            dto.setStatus(user.getStatus());
            allCustomers.add(dto);
        }
        return  customersRespository.saveAllCustomers(allCustomers);
    }
}
