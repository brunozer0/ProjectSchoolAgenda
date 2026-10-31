package com.schoolagenda.service;

import com.schoolagenda.domain.entity.*;
import com.schoolagenda.domain.enums.*;
import com.schoolagenda.dto.note.NoteUpdateRequest;
import com.schoolagenda.exception.*;
import com.schoolagenda.repository.*;
import com.schoolagenda.infrastructure.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotePermissionsTest {
    @Mock NoteRepository notes;
    @Mock StudentRepository students;
    @Mock TeacherRepository teachers;
    @Mock ResponsibleRepository responsibles;
    @Mock ResponsibleStudentRepository links;
    @Mock UserRepository users;
    @Mock StorageService storage;
    @Mock ImageRepository images;
    @Mock NoteReadRepository reads;
    @InjectMocks NoteService service;
    User requester;
    Student student;
    Note note;

    @BeforeEach
    void setup() {
        requester = user(10L, RoleName.TEACHER);
        student = new Student(); student.setId(100L); student.setName("Aluno");
        note = new Note(); note.setId(900L); note.setStudent(student);
        note.setAuthor(user(20L, RoleName.RESPONSIBLE));
        note.setContent("Mensagem original"); note.setTitle("Titulo");
        note.setType(NoteType.GENERAL); note.setVisibleToResponsible(true);
        when(users.findByEmailAndDeletedAtIsNull("usuario@escola.com")).thenReturn(Optional.of(requester));
        when(notes.findById(900L)).thenReturn(Optional.of(note));
    }

    private static User user(Long id, RoleName role) {
        User user = new User(); user.setId(id); user.setName("Usuario"); user.setRole(new Role(role));
        return user;
    }
    private void teacherAccess(boolean allowed) {
        Teacher teacher = new Teacher(); teacher.setId(10L);
        when(teachers.findByUserId(10L)).thenReturn(Optional.of(teacher));
        when(students.existsByStudentAndTeacher(100L, 10L)).thenReturn(allowed);
    }
    private void responsibleAccess(boolean allowed) {
        requester.setRole(new Role(RoleName.RESPONSIBLE));
        Responsible responsible = new Responsible(); responsible.setId(10L);
        when(responsibles.findByUserId(10L)).thenReturn(Optional.of(responsible));
        when(links.existsByResponsibleIdAndStudentId(10L, 100L)).thenReturn(allowed);
    }

    @Test void teacherCanResolveReceivedNoteWithoutChangingContent() {
        teacherAccess(true);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setStatus(NoteStatus.RESOLVED);
        when(notes.save(note)).thenReturn(note);
        var response = service.update(900L, request, "usuario@escola.com");
        assertEquals(NoteStatus.RESOLVED, response.getStatus());
        assertEquals("Mensagem original", response.getContent());
    }
    @Test void mixedContentAndStatusRequestIsRejectedWithoutPartialUpdate() {
        teacherAccess(true);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setStatus(NoteStatus.RESOLVED);
        request.setContent("Tentativa de alteracao");
        assertThrows(ForbiddenException.class, () -> service.update(900L, request, "usuario@escola.com"));
        assertEquals(NoteStatus.OPEN, note.getStatus());
        assertEquals("Mensagem original", note.getContent());
        verify(notes, never()).save(any());
    }
    @Test void teacherOutsideClassroomCannotResolveNote() {
        teacherAccess(false);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setStatus(NoteStatus.RESOLVED);
        assertThrows(ForbiddenException.class, () -> service.update(900L, request, "usuario@escola.com"));
        verify(notes, never()).save(any());
    }
    @Test void responsibleCannotEditTeacherContent() {
        responsibleAccess(true);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setContent("Alterado");
        assertThrows(ForbiddenException.class, () -> service.update(900L, request, "usuario@escola.com"));
        verify(notes, never()).save(any());
    }
    @Test void responsibleCanEditOwnVisibleNote() {
        responsibleAccess(true); note.setAuthor(requester);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setContent("Alterado");
        when(notes.save(note)).thenReturn(note);
        assertEquals("Alterado", service.update(900L, request, "usuario@escola.com").getContent());
    }
    @Test void linkedResponsibleCanChangeStatusWithoutEditingContent() {
        responsibleAccess(true);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setStatus(NoteStatus.RESOLVED);
        when(notes.save(note)).thenReturn(note);
        assertEquals(NoteStatus.RESOLVED, service.update(900L, request, "usuario@escola.com").getStatus());
    }
    @Test void authorTeacherCanStillEditOwnContent() {
        teacherAccess(true); note.setAuthor(requester);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setContent("Alterado");
        when(notes.save(note)).thenReturn(note);
        assertEquals("Alterado", service.update(900L, request, "usuario@escola.com").getContent());
    }
    @Test void formerTeacherCannotEditEvenOwnNote() {
        teacherAccess(false); note.setAuthor(requester);
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setContent("Alterado");
        assertThrows(ForbiddenException.class, () -> service.update(900L, request, "usuario@escola.com"));
    }
    @Test void teacherCanReadReceivedNoteWithoutMarkingItRead() {
        teacherAccess(true);
        assertEquals(900L, service.findById(900L, "usuario@escola.com").getId());
        verifyNoInteractions(reads);
    }
    @Test void linkedResponsibleCannotReadHiddenNote() {
        responsibleAccess(true); note.setVisibleToResponsible(false);
        assertThrows(ForbiddenException.class, () -> service.findById(900L, "usuario@escola.com"));
        assertThrows(ForbiddenException.class, () -> service.markAsRead(900L, "usuario@escola.com"));
        NoteUpdateRequest request = new NoteUpdateRequest(); request.setStatus(NoteStatus.RESOLVED);
        assertThrows(ForbiddenException.class, () -> service.update(900L, request, "usuario@escola.com"));
        verifyNoInteractions(reads);
        verify(notes, never()).save(any());
    }
    @Test void unlinkedResponsibleCannotMarkAsRead() {
        responsibleAccess(false);
        assertThrows(ForbiddenException.class, () -> service.markAsRead(900L, "usuario@escola.com"));
        verifyNoInteractions(reads);
    }
    @Test void unrelatedTeacherCannotMarkAsRead() {
        teacherAccess(false);
        assertThrows(ForbiddenException.class, () -> service.markAsRead(900L, "usuario@escola.com"));
        verifyNoInteractions(reads);
    }
    @ParameterizedTest @EnumSource(RoleName.class)
    void authorizedUserCanMarkAsReadIndependentlyOfBusinessStatus(RoleName role) {
        requester.setRole(new Role(role));
        if (role == RoleName.TEACHER) teacherAccess(true);
        if (role == RoleName.RESPONSIBLE) responsibleAccess(true);
        LocalDateTime firstRead = LocalDateTime.of(2026, 1, 1, 12, 0);
        when(reads.markAsRead(900L, 10L)).thenReturn(firstRead);
        var response = service.markAsRead(900L, "usuario@escola.com");
        assertTrue(response.read()); assertEquals(firstRead, response.readAt());
        assertEquals(NoteStatus.OPEN, note.getStatus());
        verify(reads).markAsRead(900L, 10L);
        verify(notes, never()).save(any());
    }
    @Test void readStatusDoesNotMarkOrChangeNote() {
        teacherAccess(true);
        var response = service.getReadStatus(900L, "usuario@escola.com");
        assertFalse(response.read()); assertNull(response.readAt());
        verify(reads, never()).markAsRead(anyLong(), anyLong());
        verify(notes, never()).save(any());
    }
    @ParameterizedTest @EnumSource(RoleName.class)
    void deletedStudentBlocksAllNoteOperationsIncludingDirector(RoleName role) {
        requester.setRole(new Role(role)); student.setDeletedAt(LocalDateTime.now());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(900L, "usuario@escola.com"));
        assertThrows(ResourceNotFoundException.class, () -> service.update(900L, new NoteUpdateRequest(), "usuario@escola.com"));
        assertThrows(ResourceNotFoundException.class, () -> service.delete(900L, "usuario@escola.com"));
        assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(900L, "usuario@escola.com"));
        assertThrows(ResourceNotFoundException.class, () -> service.getReadStatus(900L, "usuario@escola.com"));
        verifyNoInteractions(reads, storage, images);
        verify(notes, never()).save(any());
    }
    @Test void deletedNoteCannotBeReadOrMarked() {
        note.setDeletedAt(LocalDateTime.now());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(900L, "usuario@escola.com"));
        assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(900L, "usuario@escola.com"));
        verifyNoInteractions(reads);
    }
}
