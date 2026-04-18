package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ResponsibleRepository responsibleRepository;
    @Mock
    private ResponsibleStudentRepository responsibleStudentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudentService studentService;

    private User teacherUser;
    private User responsibleUser;
    private User directorUser;
    private Student activeStudent;

    @BeforeEach
    void setup() {
        teacherUser = buildUser(10L, "teacher@school.com", RoleName.TEACHER);
        responsibleUser = buildUser(20L, "responsible@school.com", RoleName.RESPONSIBLE);
        directorUser = buildUser(30L, "director@school.com", RoleName.DIRECTOR);
        activeStudent = buildStudent(100L, "Aluno 1", null);
    }

    @Test
    void listMyStudentsAsTeacherReturnsStudents() {
        Teacher teacher = new Teacher();
        teacher.setId(teacherUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        when(studentRepository.findAllByTeacherId(teacher.getId())).thenReturn(List.of(activeStudent));

        List<StudentResponse> result = studentService.listMyStudentsAsTeacher(teacherUser.getEmail());

        assertEquals(1, result.size());
        assertEquals(activeStudent.getId(), result.get(0).getId());
    }

    @Test
    void listMyStudentsAsTeacherRejectsWrongRole() {
        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));

        assertThrows(ForbiddenException.class,
                () -> studentService.listMyStudentsAsTeacher(directorUser.getEmail()));
    }

    @Test
    void listMyStudentsAsResponsibleReturnsStudents() {
        Responsible responsible = new Responsible();
        responsible.setId(responsibleUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(responsibleUser.getEmail())).thenReturn(Optional.of(responsibleUser));
        when(responsibleRepository.findByUserId(responsibleUser.getId())).thenReturn(Optional.of(responsible));
        when(studentRepository.findAllByResponsibleId(responsible.getId())).thenReturn(List.of(activeStudent));

        List<StudentResponse> result = studentService.listMyStudentsAsResponsible(responsibleUser.getEmail());

        assertEquals(1, result.size());
        assertEquals(activeStudent.getId(), result.get(0).getId());
    }

    @Test
    void listMyStudentsAsResponsibleRejectsWrongRole() {
        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));

        assertThrows(ForbiddenException.class,
                () -> studentService.listMyStudentsAsResponsible(teacherUser.getEmail()));
    }

    @Test
    void findByIdDirectorHasFullAccess() {
        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));

        StudentResponse result = studentService.findById(activeStudent.getId(), directorUser.getEmail());

        assertEquals(activeStudent.getId(), result.getId());
    }

    @Test
    void findByIdTeacherCanAccessOwnStudent() {
        Teacher teacher = new Teacher();
        teacher.setId(teacherUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByIdAndClassroomTeacherIdAndDeletedAtIsNull(activeStudent.getId(), teacher.getId()))
                .thenReturn(true);

        StudentResponse result = studentService.findById(activeStudent.getId(), teacherUser.getEmail());

        assertEquals(activeStudent.getId(), result.getId());
    }

    @Test
    void findByIdTeacherCannotAccessStudentFromOtherClassroom() {
        Teacher teacher = new Teacher();
        teacher.setId(teacherUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByIdAndClassroomTeacherIdAndDeletedAtIsNull(activeStudent.getId(), teacher.getId()))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> studentService.findById(activeStudent.getId(), teacherUser.getEmail()));
    }

    @Test
    void findByIdResponsibleCanAccessLinkedStudent() {
        Responsible responsible = new Responsible();
        responsible.setId(responsibleUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(responsibleUser.getEmail())).thenReturn(Optional.of(responsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleRepository.findByUserId(responsibleUser.getId())).thenReturn(Optional.of(responsible));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(true);

        StudentResponse result = studentService.findById(activeStudent.getId(), responsibleUser.getEmail());

        assertEquals(activeStudent.getId(), result.getId());
    }

    @Test
    void findByIdResponsibleCannotAccessUnlinkedStudent() {
        Responsible responsible = new Responsible();
        responsible.setId(responsibleUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(responsibleUser.getEmail())).thenReturn(Optional.of(responsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleRepository.findByUserId(responsibleUser.getId())).thenReturn(Optional.of(responsible));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> studentService.findById(activeStudent.getId(), responsibleUser.getEmail()));
    }

    @Test
    void findByIdThrowsNotFoundWhenStudentIsSoftDeleted() {
        Student deletedStudent = buildStudent(200L, "Aluno Excluido", LocalDateTime.now());

        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));
        when(studentRepository.findById(deletedStudent.getId())).thenReturn(Optional.of(deletedStudent));

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.findById(deletedStudent.getId(), directorUser.getEmail()));
    }

    @Test
    void findByIdThrowsNotFoundWhenStudentDoesNotExist() {
        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.findById(999L, directorUser.getEmail()));
    }

    @Test
    void findByIdThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByEmailAndDeletedAtIsNull("missing@school.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.findById(activeStudent.getId(), "missing@school.com"));
        verify(userRepository).findByEmailAndDeletedAtIsNull("missing@school.com");
    }

    private static User buildUser(Long id, String email, RoleName roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("User " + id);
        user.setRole(new Role(roleName));
        return user;
    }

    private static Student buildStudent(Long id, String name, LocalDateTime deletedAt) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setDeletedAt(deletedAt);
        return student;
    }
}
