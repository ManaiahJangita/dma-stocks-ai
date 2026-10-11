package com.stockai.service.impl;

import com.stockai.dto.CustomerDto;
import com.stockai.dto.CustomersFilterDto;
import java.util.stream.Collectors;
import com.stockai.entity.CustomerEntity;
import com.stockai.entity.User;
import com.stockai.repository.CustomersRespository;
import com.stockai.repository.UserRepository;
import com.stockai.service.interf.CustomersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
// ...existing code...
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomersServiceImpl implements CustomersService {

    @Autowired
    private CustomersRespository customersRespository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<CustomerDto> getCustomersFilters(CustomersFilterDto filters) {
        List<CustomerEntity> allCustomers = customersRespository.findCustomers(filters);

        return allCustomers.stream().map(customer -> {
            CustomerDto dto = new CustomerDto();
            BeanUtils.copyProperties(customer, dto);
            return dto;
        }).toList();
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

    @Transactional
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
            // convert user roles to comma separated string and store in customer entity
            try {
                if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                    String rolesCsv = user.getRoles().stream()
                            .map(com.stockai.entity.UserRoles::getRoleName)
                            .distinct()
                            .collect(Collectors.joining(","));
                    dto.setRoles(rolesCsv);
                } else {
                    dto.setRoles(null);
                }
            } catch (Exception ex) {
                dto.setRoles(null);
            }
            allCustomers.add(dto);
        }
        return  customersRespository.saveAllCustomers(allCustomers);
    }
}
