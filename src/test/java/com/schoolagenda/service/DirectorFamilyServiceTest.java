package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.Gender;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.director.FamilyChildRequest;
import com.schoolagenda.dto.director.FamilyCreateRequest;
import com.schoolagenda.dto.director.FamilyCreateResponse;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import com.schoolagenda.repository.ResponsibleRepository;
import com.schoolagenda.repository.ResponsibleStudentRepository;
import com.schoolagenda.repository.RoleRepository;
import com.schoolagenda.repository.StudentRepository;
import com.schoolagenda.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectorFamilyServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ResponsibleRepository responsibleRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ResponsibleStudentRepository responsibleStudentRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DirectorFamilyService directorFamilyService;

    @Test
    void createFamilyCreatesResponsibleChildrenAndLinks() {
        FamilyCreateRequest request = buildRequest();
        Role responsibleRole = new Role(RoleName.RESPONSIBLE);

        User savedUser = new User();
        savedUser.setId(11L);
        savedUser.setName("Ana Souza");
        savedUser.setEmail(request.getEmail());
        savedUser.setRole(responsibleRole);

        Responsible savedResponsible = new Responsible();
        savedResponsible.setId(savedUser.getId());
        savedResponsible.setUser(savedUser);

        Classroom classroom = new Classroom();
        classroom.setId(5L);

        Student savedStudent = new Student();
        savedStudent.setId(30L);
        savedStudent.setName("Lucas");
        savedStudent.setClassroom(classroom);
        savedStudent.setBirthDate(LocalDate.of(2018, 5, 10));
        savedStudent.setGender(Gender.MALE);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.RESPONSIBLE)).thenReturn(Optional.of(responsibleRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(responsibleRepository.save(any(Responsible.class))).thenReturn(savedResponsible);
        when(classroomRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(classroom));
        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        FamilyCreateResponse response = directorFamilyService.createFamily(request);

        assertEquals(savedResponsible.getId(), response.getResponsibleId());
        assertEquals(savedUser.getId(), response.getUserId());
        assertEquals(1, response.getStudentIds().size());
        assertEquals(savedStudent.getId(), response.getStudentIds().get(0));

        verify(responsibleStudentRepository).save(any());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded", userCaptor.getValue().getPassword());
    }

    @Test
    void createFamilyThrowsConflictWhenEmailExists() {
        FamilyCreateRequest request = buildRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> directorFamilyService.createFamily(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createFamilyThrowsNotFoundWhenClassroomDoesNotExist() {
        FamilyCreateRequest request = buildRequest();
        Role responsibleRole = new Role(RoleName.RESPONSIBLE);
        User savedUser = new User();
        savedUser.setId(11L);
        savedUser.setRole(responsibleRole);
        Responsible savedResponsible = new Responsible();
        savedResponsible.setId(11L);
        savedResponsible.setUser(savedUser);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.RESPONSIBLE)).thenReturn(Optional.of(responsibleRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(responsibleRepository.save(any(Responsible.class))).thenReturn(savedResponsible);
        when(classroomRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> directorFamilyService.createFamily(request));
    }

    private static FamilyCreateRequest buildRequest() {
        FamilyChildRequest child = new FamilyChildRequest();
        child.setName("Lucas");
        child.setGender(Gender.MALE);
        child.setBirthDate(LocalDate.of(2018, 5, 10));
        child.setClassroomId(5L);

        FamilyCreateRequest request = new FamilyCreateRequest();
        request.setFirstName("Ana");
        request.setLastName("Souza");
        request.setEmail("ana.souza@escola.com");
        request.setPassword("123456");
        request.setChildren(List.of(child));
        return request;
    }
}
