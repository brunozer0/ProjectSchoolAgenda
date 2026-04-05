package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Responsible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ResponsibleRepository extends JpaRepository<Responsible, Long> {
    Optional<Responsible> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}