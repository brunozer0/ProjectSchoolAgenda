package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.ResponsibleStudent;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ResponsibleRepository;
import com.schoolagenda.repository.ResponsibleStudentRepository;
import com.schoolagenda.repository.StudentRepository;
import com.schoolagenda.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResponsibleStudentService {

    private final ResponsibleRepository responsibleRepository;
    private final ResponsibleStudentRepository responsibleStudentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public void linkResponsibleToStudent(Long responsibleId, Long studentId) {
        Responsible responsible = getActiveResponsible(responsibleId);
        Student student = getActiveStudent(studentId);

        if (responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), student.getId())) {
            throw new ConflictException("Vinculo entre responsavel e aluno ja existe.");
        }

        ResponsibleStudent link = new ResponsibleStudent();
        link.setResponsible(responsible);
        link.setStudent(student);
        responsibleStudentRepository.save(link);
    }

    @Transactional
    public void unlinkResponsibleFromStudent(Long responsibleId, Long studentId) {
        getActiveResponsible(responsibleId);
        getActiveStudent(studentId);

        if (!responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsibleId, studentId)) {
            throw new ResourceNotFoundException("Vinculo entre responsavel e aluno nao encontrado.");
        }

        responsibleStudentRepository.deleteByResponsibleIdAndStudentId(responsibleId, studentId);
    }

    private Responsible getActiveResponsible(Long responsibleId) {
        Responsible responsible = responsibleRepository.findById(responsibleId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado"));

        userRepository.findByIdAndDeletedAtIsNull(responsible.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado"));

        return responsible;
    }

    private Student getActiveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado"));

        if (student.isDeleted()) {
            throw new ResourceNotFoundException("Aluno nao encontrado");
        }

        return student;
    }
}
