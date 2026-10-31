package com.schoolagenda.service;

import com.schoolagenda.domain.entity.*;
import com.schoolagenda.exception.ForbiddenException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {
    @Mock ClassroomRepository classroomRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock UserRepository userRepository;
    @Mock StudentRepository studentRepository;
    @InjectMocks ClassroomService service;
    User user;
    Teacher teacher;
    Classroom classroom;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(10L);
        user.setName("Professor");
        teacher = new Teacher();
        teacher.setId(10L);
        teacher.setUser(user);
        classroom = new Classroom();
        classroom.setId(1L);
        classroom.setName("Turma A");
    }

    private void prepareClassroomAccess() {
        when(userRepository.findByEmailAndDeletedAtIsNull("professor@escola.com"))
                .thenReturn(Optional.of(user));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(classroomRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(classroom));
    }

    @Test
    void teacherOutsideClassroomCannotReadStudents() {
        prepareClassroomAccess();
        assertThrows(ForbiddenException.class,
                () -> service.getStudentsFromClassroom(1L, "professor@escola.com"));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void linkedTeacherCanReadActiveStudents() {
        prepareClassroomAccess();
        classroom.setTeachers(List.of(teacher));
        Student student = new Student();
        student.setId(100L);
        student.setName("Aluno");
        when(studentRepository.findAllByClassroomIdAndDeletedAtIsNull(1L)).thenReturn(List.of(student));
        var response = service.getStudentsFromClassroom(1L, "professor@escola.com");
        assertEquals(1L, response.getClassroom().getId());
        assertEquals(100L, response.getStudents().get(0).getId());
        assertEquals(10L, response.getTeachers().get(0).getId());
    }

    @Test
    void linkedTeacherCanReadEmptyClassroom() {
        prepareClassroomAccess();
        classroom.setTeachers(List.of(teacher));
        assertTrue(service.getStudentsFromClassroom(1L, "professor@escola.com").getStudents().isEmpty());
    }

    @Test
    void teacherDoesNotSeeDeletedClassrooms() {
        Classroom deleted = new Classroom();
        deleted.setId(2L);
        deleted.setDeletedAt(LocalDateTime.now());
        teacher.setClassrooms(List.of(classroom, deleted));
        when(userRepository.findByEmailAndDeletedAtIsNull("professor@escola.com"))
                .thenReturn(Optional.of(user));
        when(teacherRepository.findByIdWithUserAndClassrooms(10L)).thenReturn(Optional.of(teacher));
        var response = service.getMyClassrooms("professor@escola.com");
        assertEquals(10L, response.getTeacherId());
        assertEquals(1, response.getClassrooms().size());
        assertEquals(1L, response.getClassrooms().get(0).getId());
    }

    @Test
    void missingUserCannotReadClassrooms() {
        assertThrows(ResourceNotFoundException.class, () -> service.getMyClassrooms("ausente@escola.com"));
        verifyNoInteractions(teacherRepository, classroomRepository, studentRepository);
    }

    @Test
    void userWithoutTeacherRecordCannotReadStudents() {
        when(userRepository.findByEmailAndDeletedAtIsNull("professor@escola.com"))
                .thenReturn(Optional.of(user));
        assertThrows(ResourceNotFoundException.class,
                () -> service.getStudentsFromClassroom(1L, "professor@escola.com"));
        verifyNoInteractions(classroomRepository, studentRepository);
    }

    @Test
    void missingOrDeletedClassroomCannotBeRead() {
        when(userRepository.findByEmailAndDeletedAtIsNull("professor@escola.com"))
                .thenReturn(Optional.of(user));
        when(teacherRepository.findById(10L)).thenReturn(Optional.of(teacher));
        assertThrows(ResourceNotFoundException.class,
                () -> service.getStudentsFromClassroom(99L, "professor@escola.com"));
        verifyNoInteractions(studentRepository);
    }
}
