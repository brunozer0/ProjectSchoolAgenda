package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.student.StudentResponse;
import com.schoolagenda.exception.ForbiddenException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ResponsibleRepository;
import com.schoolagenda.repository.ResponsibleStudentRepository;
import com.schoolagenda.repository.StudentRepository;
import com.schoolagenda.repository.TeacherRepository;
import com.schoolagenda.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ResponsibleRepository responsibleRepository;
    private final ResponsibleStudentRepository responsibleStudentRepository;
    private final UserRepository userRepository;

    public List<StudentResponse> listMyStudentsAsTeacher(String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        ensureRole(requester, RoleName.TEACHER);

        Teacher teacher = teacherRepository.findByUserId(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        return studentRepository.findAllByTeacherId(teacher.getId())
                .stream()
                .map(StudentResponse::from)
                .toList();
    }

    public List<StudentResponse> listMyStudentsAsResponsible(String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        ensureRole(requester, RoleName.RESPONSIBLE);

        Responsible responsible = responsibleRepository.findByUserId(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado"));

        return studentRepository.findAllByResponsibleId(responsible.getId())
                .stream()
                .map(StudentResponse::from)
                .toList();
    }

    public StudentResponse findById(Long studentId, String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        Student student = getActiveStudent(studentId);
        RoleName role = requester.getRole().getName();

        if (role == RoleName.DIRECTOR) {
            return StudentResponse.from(student);
        }

        if (role == RoleName.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

            if (!studentRepository.existsByStudentAndTeacher(
                    student.getId(), teacher.getId())) {
                throw new ForbiddenException("O aluno nao pertence a sua turma.");
            }

            return StudentResponse.from(student);
        }

        if (role == RoleName.RESPONSIBLE) {
            Responsible responsible = responsibleRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado"));

            if (!responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), student.getId())) {
                throw new ForbiddenException("Acesso negado ao aluno informado.");
            }

            return StudentResponse.from(student);
        }

        throw new ForbiddenException("Perfil sem permissao para acessar alunos.");
    }

    private User getActiveUser(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }

    private Student getActiveStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado"));

        if (student.isDeleted()) {
            throw new ResourceNotFoundException("Aluno nao encontrado");
        }

        return student;
    }

    private void ensureRole(User user, RoleName expectedRole) {
        if (user.getRole().getName() != expectedRole) {
            throw new ForbiddenException("Perfil sem permissao para esta operacao.");
        }
    }
}
