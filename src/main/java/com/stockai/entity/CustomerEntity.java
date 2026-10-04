package com.stockai.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "customers", schema = "dmastock")
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable= false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String phone;

    @Column
    private String status;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name="updated_date", nullable = false)
    private LocalDate updatedDate;

}
