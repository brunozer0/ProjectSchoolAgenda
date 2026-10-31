package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.dto.director.ClassroomCreateRequest;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DirectorClassroomServiceTest {
    @Mock ClassroomRepository repository;
    @InjectMocks DirectorClassroomService service;

    @Test
    void createsClassroomWhenNameIsAvailable() {
        ClassroomCreateRequest request = new ClassroomCreateRequest();
        request.setName("Turma A");
        when(repository.save(any(Classroom.class))).thenAnswer(invocation -> {
            Classroom classroom = invocation.getArgument(0);
            classroom.setId(1L);
            return classroom;
        });
        var response = service.createClassroom(request);
        assertEquals(1L, response.getId());
        assertEquals("Turma A", response.getName());
    }

    @Test
    void duplicateActiveNameDoesNotCreateClassroom() {
        ClassroomCreateRequest request = new ClassroomCreateRequest();
        request.setName("Turma A");
        when(repository.existsByNameAndDeletedAtIsNull("Turma A")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.createClassroom(request));
        verify(repository, never()).save(any());
    }

    @Test
    void deletionPreservesClassroomAndRecordsDeletionTime() {
        Classroom classroom = new Classroom();
        classroom.setId(1L);
        classroom.setName("Turma A");
        when(repository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(classroom));
        LocalDateTime before = LocalDateTime.now();
        service.deleteClassroom(1L);
        assertNotNull(classroom.getDeletedAt());
        assertFalse(classroom.getDeletedAt().isBefore(before));
        assertFalse(classroom.getDeletedAt().isAfter(LocalDateTime.now()));
        assertEquals("Turma A", classroom.getName());
        verify(repository).save(classroom);
        verify(repository, never()).delete(any());
    }

    @Test
    void missingOrAlreadyDeletedClassroomCannotBeDeleted() {
        assertThrows(ResourceNotFoundException.class, () -> service.deleteClassroom(99L));
        verify(repository, never()).save(any());
    }

    @Test
    void listsActiveClassrooms() {
        Classroom classroom = new Classroom();
        classroom.setId(1L);
        classroom.setName("Turma A");
        when(repository.findAllByDeletedAtIsNull()).thenReturn(List.of(classroom));
        var result = service.listClassrooms();
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Turma A", result.get(0).getName());
    }
}
