package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ResponsibleRepository;
import com.schoolagenda.repository.ResponsibleStudentRepository;
import com.schoolagenda.repository.StudentRepository;
import com.schoolagenda.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponsibleStudentServiceTest {

    @Mock
    private ResponsibleRepository responsibleRepository;
    @Mock
    private ResponsibleStudentRepository responsibleStudentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResponsibleStudentService responsibleStudentService;

    private Responsible responsible;
    private Student activeStudent;
    private User activeResponsibleUser;

    @BeforeEach
    void setup() {
        responsible = new Responsible();
        responsible.setId(10L);

        activeStudent = new Student();
        activeStudent.setId(20L);
        activeStudent.setDeletedAt(null);

        activeResponsibleUser = new User();
        activeResponsibleUser.setId(10L);
    }

    @Test
    void linkResponsibleToStudentCreatesLinkWhenDataIsValid() {
        when(responsibleRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        when(userRepository.findByIdAndDeletedAtIsNull(responsible.getId())).thenReturn(Optional.of(activeResponsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(false);

        responsibleStudentService.linkResponsibleToStudent(responsible.getId(), activeStudent.getId());

        verify(responsibleStudentRepository).save(any());
    }

    @Test
    void linkResponsibleToStudentThrowsConflictWhenLinkAlreadyExists() {
        when(responsibleRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        when(userRepository.findByIdAndDeletedAtIsNull(responsible.getId())).thenReturn(Optional.of(activeResponsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> responsibleStudentService.linkResponsibleToStudent(responsible.getId(), activeStudent.getId()));

        verify(responsibleStudentRepository, never()).save(any());
    }

    @Test
    void unlinkResponsibleFromStudentRemovesLinkWhenItExists() {
        when(responsibleRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        when(userRepository.findByIdAndDeletedAtIsNull(responsible.getId())).thenReturn(Optional.of(activeResponsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(true);

        responsibleStudentService.unlinkResponsibleFromStudent(responsible.getId(), activeStudent.getId());

        verify(responsibleStudentRepository).deleteByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId());
    }

    @Test
    void unlinkResponsibleFromStudentThrowsNotFoundWhenLinkDoesNotExist() {
        when(responsibleRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        when(userRepository.findByIdAndDeletedAtIsNull(responsible.getId())).thenReturn(Optional.of(activeResponsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> responsibleStudentService.unlinkResponsibleFromStudent(responsible.getId(), activeStudent.getId()));
    }

    @Test
    void linkResponsibleToStudentThrowsNotFoundWhenStudentIsDeleted() {
        Student deletedStudent = new Student();
        deletedStudent.setId(21L);
        deletedStudent.setDeletedAt(LocalDateTime.now());

        when(responsibleRepository.findById(responsible.getId())).thenReturn(Optional.of(responsible));
        when(userRepository.findByIdAndDeletedAtIsNull(responsible.getId())).thenReturn(Optional.of(activeResponsibleUser));
        when(studentRepository.findById(deletedStudent.getId())).thenReturn(Optional.of(deletedStudent));

        assertThrows(ResourceNotFoundException.class,
                () -> responsibleStudentService.linkResponsibleToStudent(responsible.getId(), deletedStudent.getId()));
    }
}
