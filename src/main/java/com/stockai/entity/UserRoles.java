package com.stockai.entity;

import com.stockai.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Data
@Table(name = "user_roles", schema = "dmastock")
public class UserRoles {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String roleName;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}