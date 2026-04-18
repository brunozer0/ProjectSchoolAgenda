package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    Optional<Classroom> findByIdAndDeletedAtIsNull(Long id);

    List<Classroom> findAllByTeacherIdAndDeletedAtIsNull(Long teacherId);
}
