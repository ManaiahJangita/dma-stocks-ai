package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users" , schema = "dmastock")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private long mobileNumber;

    @Column(nullable = false, unique = true)
    private String email;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String image;

    @Column(nullable = false, unique = true)
    private String status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRoles> roles;

    @Column(name = "created_date",nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date",nullable = false)
    private LocalDate updatedDate;
}