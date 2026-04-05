package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
        SELECT t FROM Teacher t
        JOIN FETCH t.user
        WHERE t.user.id = :userId
    """)
    Optional<Teacher> findWithUserByUserId(@Param("userId") Long userId);
}