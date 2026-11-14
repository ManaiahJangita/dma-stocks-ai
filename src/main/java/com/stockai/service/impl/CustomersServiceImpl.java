package com.stockai.service.impl;

import com.stockai.dto.CustomerDto;
import com.stockai.dto.CustomersFilterDto;
import com.stockai.entity.CustomerEntity;
import com.stockai.entity.User;
import com.stockai.repository.CustomersRespository;
import com.stockai.repository.UserRepository;
import com.stockai.service.interf.CustomersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomersServiceImpl implements CustomersService {

    @Autowired
    private CustomersRespository customersRespository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<CustomerDto> getCustomersFilters(CustomersFilterDto filters) {
        log.info("Fetching customers with filters: {}", filters);
        List<CustomerEntity> allCustomers = customersRespository.findCustomers(filters);
        List<CustomerDto> result = allCustomers.stream().map(customer -> {
            CustomerDto dto = new CustomerDto();
            BeanUtils.copyProperties(customer, dto);
            return dto;
        }).toList();
        log.info("Customer filter request completed with {} records", result.size());
        return result;
    }

    @Override
    public List<CustomerDto> fetchAllCustomersInfo() {
        log.info("Fetching all customer information");
        List<CustomerEntity> allCustomers = customersRespository.fetchAllCustomers();
        List<CustomerDto> customerDto = new ArrayList<>();
        for (CustomerEntity entity : allCustomers) {
            CustomerDto dto = new CustomerDto();
            BeanUtils.copyProperties(entity, dto);
            customerDto.add(dto);
        }
        log.info("Fetched {} customer records", customerDto.size());
        return customerDto;
    }

    @Transactional
    public int syncAllCustomersInfo() {
        log.info("Starting sync of all customer data from users");
        List<User> allUsers = userRepository.findAll();
        log.info("Loaded {} users for customer sync", allUsers.size());
        List<CustomerEntity> allCustomers = new ArrayList<>(allUsers.size());
        for (User user : allUsers) {
            CustomerEntity dto = new CustomerEntity();
            dto.setUserId(user.getId());
            dto.setName(user.getFirstName() + " " + user.getLastName());
            dto.setPhone(String.valueOf(user.getMobileNumber()));
            dto.setEmail(user.getEmail());
            dto.setCreatedDate(user.getCreatedDate());
            dto.setUpdatedDate(user.getUpdatedDate());
            dto.setStatus(user.getStatus());
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
                log.warn("Unable to map roles for user {}. Setting roles to null", user.getEmail(), ex);
                dto.setRoles(null);
            }
            allCustomers.add(dto);
        }
        int savedCount = customersRespository.saveAllCustomers(allCustomers);
        log.info("Customer sync completed. Saved {} customer records", savedCount);
        return savedCount;
    }
}
