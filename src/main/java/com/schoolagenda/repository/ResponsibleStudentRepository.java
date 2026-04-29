package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.ResponsibleStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponsibleStudentRepository extends JpaRepository<ResponsibleStudent, Long> {

    boolean existsByResponsibleIdAndStudentId(Long responsibleId, Long studentId);



    void deleteByResponsibleIdAndStudentId(Long responsibleId, Long studentId);

    void deleteByResponsibleId(long resposibleId);

    List<ResponsibleStudent> findByResponsibleId(Long responsibleId);

    List<ResponsibleStudent> findByStudentId(Long studentId);
}