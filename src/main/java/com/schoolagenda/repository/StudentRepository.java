package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
        SELECT s FROM Student s
        JOIN s.classroom c
        JOIN c.teachers t
        WHERE t.id = :teacherId
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

    List<Student> findAllByClassroomIdAndDeletedAtIsNull(Long classroomId);

    @Query("""
        SELECT COUNT(s) > 0 FROM Student s
        JOIN s.classroom c
        JOIN c.teachers t
        WHERE s.id = :studentId
        AND t.id = :teacherId
        AND s.deletedAt IS NULL
    """)
    boolean existsByStudentAndTeacher(Long studentId, Long teacherId);
}