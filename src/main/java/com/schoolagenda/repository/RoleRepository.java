package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
