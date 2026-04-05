package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
        SELECT s FROM Student s
        WHERE s.classroom.teacher.id = :teacherId
        AND s.deletedAt IS NULL
    """)
    List<Student> findAllByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
        SELECT s FROM Student s
        JOIN ResponsibleStudent rs ON rs.student = s
        WHERE rs.responsible.id = :responsibleId
        AND s.deletedAt IS NULL
    """)
    List<Student> findAllByResponsibleId(@Param("responsibleId") Long responsibleId);

    boolean existsByIdAndClassroomTeacherIdAndDeletedAtIsNull(Long studentId, Long teacherId);
}