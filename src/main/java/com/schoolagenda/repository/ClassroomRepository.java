package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findAllByDeletedAtIsNull();

    Optional<Classroom> findByIdAndDeletedAtIsNull(Long id);

    List<Classroom> findByTeachers_IdAndDeletedAtIsNull(Long teacherId);

    boolean existsByNameAndDeletedAtIsNull(String name);

}