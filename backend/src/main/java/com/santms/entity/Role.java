package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity for RBAC
 */
@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    @Column(length = 255)
    private String description;

    public enum RoleName {
        ROLE_SUPER_ADMIN,
        ROLE_NETWORK_ADMIN,
        ROLE_SECURITY_ANALYST,
        ROLE_READ_ONLY
    }
}
