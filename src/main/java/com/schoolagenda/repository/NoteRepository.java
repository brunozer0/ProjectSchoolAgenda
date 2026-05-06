package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Note;
import com.schoolagenda.domain.enums.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("""
        SELECT n FROM Note n
        WHERE n.student.id = :studentId
        AND n.deletedAt IS NULL
        AND (:status IS NULL OR n.status = :status)
        ORDER BY n.createdAt DESC
    """)
    List<Note> findByStudentId(@Param("studentId") Long studentId,
                               @Param("status") NoteStatus status);

    @Query("""
    SELECT DISTINCT n FROM Note n
    LEFT JOIN FETCH n.images
    WHERE n.student.id = :studentId
    AND n.deletedAt IS NULL
    AND (:status IS NULL OR n.status = :status)
    ORDER BY n.createdAt DESC
""")
    List<Note> findByStudentIdWithImages(@Param("studentId") Long studentId,
                                         @Param("status") NoteStatus status);

    @Query("""
        SELECT n FROM Note n
        WHERE n.student.id = :studentId
        AND n.visibleToResponsible = true
        AND n.deletedAt IS NULL
        AND (:status IS NULL OR n.status = :status)
        ORDER BY n.createdAt DESC
    """)
    List<Note> findVisibleByStudentId(@Param("studentId") Long studentId,
                                      @Param("status") NoteStatus status);

    @Query("""
    SELECT DISTINCT n FROM Note n
    LEFT JOIN FETCH n.images
    WHERE n.student.id = :studentId
    AND n.visibleToResponsible = true
    AND n.deletedAt IS NULL
    AND (:status IS NULL OR n.status = :status)
    ORDER BY n.createdAt DESC
""")
    List<Note> findVisibleByStudentIdWithImages(@Param("studentId") Long studentId,
                                                @Param("status") NoteStatus status);
}