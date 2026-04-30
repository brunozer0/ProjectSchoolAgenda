package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Responsible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResponsibleRepository extends JpaRepository<Responsible, Long> {
    Optional<Responsible> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
        SELECT DISTINCT r FROM Responsible r
        JOIN FETCH r.user u
        LEFT JOIN FETCH r.responsibleStudents rs
        LEFT JOIN FETCH rs.student s
        WHERE u.deletedAt IS NULL
        ORDER BY r.id
    """)
    List<Responsible> findAllWithStudents();
}