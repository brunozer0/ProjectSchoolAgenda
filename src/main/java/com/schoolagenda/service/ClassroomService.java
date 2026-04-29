package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.dto.classroom.ClassroomStudentsResponse;
import com.schoolagenda.dto.classroom.TeacherClassroomsResponse;
import com.schoolagenda.exception.ForbiddenException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import com.schoolagenda.repository.StudentRepository;
import com.schoolagenda.repository.TeacherRepository;
import com.schoolagenda.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public TeacherClassroomsResponse getMyClassrooms(String requesterEmail) {

        User requester = userRepository.findByEmailAndDeletedAtIsNull(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        Teacher teacher = teacherRepository.findByIdWithUserAndClassrooms(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        return TeacherClassroomsResponse.from(teacher);
    }

    public ClassroomStudentsResponse getStudentsFromClassroom(Long classroomId, String requesterEmail) {

        User requester = userRepository.findByEmailAndDeletedAtIsNull(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        Teacher teacher = teacherRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        Classroom classroom = classroomRepository
                .findByIdAndDeletedAtIsNull(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma nao encontrada"));

        boolean belongsToClassroom = classroom.getTeachers().stream()
                .anyMatch(t -> t.getId().equals(teacher.getId()));

        if (!belongsToClassroom) {
            throw new ForbiddenException("Você não tem acesso a esta turma.");
        }

        List<Student> students = studentRepository
                .findAllByClassroomIdAndDeletedAtIsNull(classroomId);

        return ClassroomStudentsResponse.from(classroom, students);
    }

}
