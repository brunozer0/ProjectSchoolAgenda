package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.director.TeacherCreateRequest;
import com.schoolagenda.dto.director.TeacherCreateResponse;
import com.schoolagenda.dto.director.TeacherListResponse;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import com.schoolagenda.repository.RoleRepository;
import com.schoolagenda.repository.TeacherRepository;
import com.schoolagenda.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectorTeacherServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DirectorTeacherService directorTeacherService;

    @Test
    void createTeacherCreatesUserTeacherAndClassrooms() {
        TeacherCreateRequest request = buildRequest();
        Role teacherRole = new Role(RoleName.TEACHER);

        User savedUser = new User();
        savedUser.setId(20L);
        savedUser.setName("Carlos Lima");
        savedUser.setEmail(request.getEmail());
        savedUser.setRole(teacherRole);

        Teacher savedTeacher = new Teacher();
        savedTeacher.setId(savedUser.getId());
        savedTeacher.setUser(savedUser);

        Classroom savedClassroom = new Classroom();
        savedClassroom.setId(30L);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(teacherRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(teacherRepository.save(any(Teacher.class))).thenReturn(savedTeacher);
        when(classroomRepository.save(any(Classroom.class))).thenReturn(savedClassroom);

        TeacherCreateResponse response = directorTeacherService.createTeacher(request);

        assertEquals(savedTeacher.getId(), response.getTeacherId());
        assertEquals(savedUser.getId(), response.getUserId());
        assertEquals(1, response.getClassroomIds().size());
        assertEquals(savedClassroom.getId(), response.getClassroomIds().get(0));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded", userCaptor.getValue().getPassword());
    }

    @Test
    void createTeacherThrowsConflictWhenEmailExists() {
        TeacherCreateRequest request = buildRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> directorTeacherService.createTeacher(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteTeacherSoftDeletesUserAndClassrooms() {
        Teacher teacher = new Teacher();
        teacher.setId(20L);

        User activeUser = new User();
        activeUser.setId(20L);

        Classroom classroom = new Classroom();
        classroom.setId(30L);

        when(teacherRepository.findById(20L)).thenReturn(Optional.of(teacher));
        when(userRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(activeUser));
        when(classroomRepository.findAllByTeacherIdAndDeletedAtIsNull(20L)).thenReturn(List.of(classroom));

        directorTeacherService.deleteTeacher(20L);

        assertNotNull(activeUser.getDeletedAt());
        assertNotNull(classroom.getDeletedAt());
        verify(userRepository).save(activeUser);
        verify(classroomRepository).saveAll(List.of(classroom));
    }

    @Test
    void deleteTeacherThrowsNotFoundWhenTeacherDoesNotExist() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> directorTeacherService.deleteTeacher(99L));
    }

    @Test
    void listTeachersReturnsActiveTeachersWithClassrooms() {
        User activeUser = new User();
        activeUser.setId(20L);
        activeUser.setName("Carlos Lima");
        activeUser.setEmail("carlos.lima@escola.com");

        Teacher teacher = new Teacher();
        teacher.setId(20L);
        teacher.setUser(activeUser);

        Classroom classroomA = new Classroom();
        classroomA.setName("Turma A");
        Classroom classroomB = new Classroom();
        classroomB.setName("Turma B");

        when(teacherRepository.findAllByUserDeletedAtIsNullOrderByUserNameAsc()).thenReturn(List.of(teacher));
        when(classroomRepository.findAllByTeacherIdAndDeletedAtIsNull(20L)).thenReturn(List.of(classroomA, classroomB));

        List<TeacherListResponse> result = directorTeacherService.listTeachers();

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getTeacherId());
        assertEquals("Carlos Lima", result.get(0).getName());
        assertEquals(List.of("Turma A", "Turma B"), result.get(0).getClassrooms());
    }

    private static TeacherCreateRequest buildRequest() {
        TeacherCreateRequest request = new TeacherCreateRequest();
        request.setFirstName("Carlos");
        request.setLastName("Lima");
        request.setEmail("carlos.lima@escola.com");
        request.setPassword("123456");
        request.setClassrooms(List.of("Turma A"));
        return request;
    }
}
