package com.schoolagenda.domain.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.schoolagenda.domain.enums.RoleName;
@Entity
@Table(
        name = "roles",
        indexes = {
                @Index(name = "idx_roles_name", columnList = "name")
        }
)
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleName name;

    public Role(RoleName name) {
        this.name = name;
    }
}