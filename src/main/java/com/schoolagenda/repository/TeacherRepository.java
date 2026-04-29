package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
        SELECT t FROM Teacher t
        JOIN FETCH t.user
        WHERE t.user.id = :userId
    """)
    Optional<Teacher> findWithUserByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT t FROM Teacher t
        JOIN FETCH t.user
        LEFT JOIN FETCH t.classrooms c
        WHERE t.user.deletedAt IS NULL
        ORDER BY t.user.name
    """)
    List<Teacher> findAllWithClassrooms();

    Long user(User user);

    @Query("""
    SELECT DISTINCT t FROM Teacher t
    JOIN FETCH t.user
    LEFT JOIN FETCH t.classrooms c
    WHERE t.id = :id
""")
    Optional<Teacher> findByIdWithUserAndClassrooms(Long id);

    @Query("""
        SELECT t FROM Teacher t
        JOIN FETCH t.user u
        WHERE t.id = :id
        AND u.deletedAt IS NULL
    """)
    Optional<Teacher> findActiveById(Long id);

}
