package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Note;
import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.domain.enums.NoteType;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.note.NoteResponse;
import com.schoolagenda.dto.note.NoteRequest;
import com.schoolagenda.dto.note.NoteUpdateRequest;
import com.schoolagenda.exception.ForbiddenException;
import com.schoolagenda.repository.NoteRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;
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
    private NoteService noteService;

    private User teacherUser;
    private User teacherAuthorUser;
    private User directorUser;
    private User responsibleUser;
    private Student activeStudent;
    private Note noteFromAnotherTeacher;

    @BeforeEach
    void setup() {
        teacherUser = buildUser(10L, "teacher@school.com", RoleName.TEACHER);
        teacherAuthorUser = buildUser(11L, "author@school.com", RoleName.TEACHER);
        directorUser = buildUser(20L, "director@school.com", RoleName.DIRECTOR);
        responsibleUser = buildUser(30L, "resp@school.com", RoleName.RESPONSIBLE);
        activeStudent = buildStudent(100L, "Aluno 1", null);
        noteFromAnotherTeacher = buildNote(900L, activeStudent, teacherAuthorUser, false, null);
    }

    @Test
    void teacherCannotListStudentOutsideOwnClassroom() {
        Teacher teacher = new Teacher();
        teacher.setId(teacherUser.getId());

        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByIdAndClassroomTeacherIdAndDeletedAtIsNull(activeStudent.getId(), teacher.getId()))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> noteService.listByStudent(activeStudent.getId(), null, teacherUser.getEmail()));
    }

    @Test
    void teacherCannotCreateNoteForStudentOutsideOwnClassroom() {
        Teacher teacher = new Teacher();
        teacher.setId(teacherUser.getId());

        NoteRequest request = new NoteRequest();
        request.setStudentId(activeStudent.getId());
        request.setContent("Conteudo");
        request.setType(NoteType.GENERAL);
        request.setVisibleToResponsible(true);

        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        when(studentRepository.existsByIdAndClassroomTeacherIdAndDeletedAtIsNull(activeStudent.getId(), teacher.getId()))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () -> noteService.create(request, teacherUser.getEmail()));
    }

    @Test
    void teacherCanOnlyEditOwnNote() {
        NoteUpdateRequest request = new NoteUpdateRequest();
        request.setContent("novo conteudo");

        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(noteRepository.findById(noteFromAnotherTeacher.getId())).thenReturn(Optional.of(noteFromAnotherTeacher));

        assertThrows(ForbiddenException.class,
                () -> noteService.update(noteFromAnotherTeacher.getId(), request, teacherUser.getEmail()));
    }

    @Test
    void teacherCanOnlyDeleteOwnNote() {
        when(userRepository.findByEmailAndDeletedAtIsNull(teacherUser.getEmail())).thenReturn(Optional.of(teacherUser));
        when(noteRepository.findById(noteFromAnotherTeacher.getId())).thenReturn(Optional.of(noteFromAnotherTeacher));

        assertThrows(ForbiddenException.class,
                () -> noteService.delete(noteFromAnotherTeacher.getId(), teacherUser.getEmail()));
    }

    @Test
    void responsibleCannotListUnlinkedStudent() {
        Responsible responsible = new Responsible();
        responsible.setId(40L);

        when(userRepository.findByEmailAndDeletedAtIsNull(responsibleUser.getEmail())).thenReturn(Optional.of(responsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleRepository.findByUserId(responsibleUser.getId())).thenReturn(Optional.of(responsible));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> noteService.listByStudent(activeStudent.getId(), null, responsibleUser.getEmail()));
    }

    @Test
    void responsibleListsOnlyVisibleNotesQuery() {
        Responsible responsible = new Responsible();
        responsible.setId(40L);
        Note visibleNote = buildNote(901L, activeStudent, teacherAuthorUser, true, null);

        when(userRepository.findByEmailAndDeletedAtIsNull(responsibleUser.getEmail())).thenReturn(Optional.of(responsibleUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(responsibleRepository.findByUserId(responsibleUser.getId())).thenReturn(Optional.of(responsible));
        when(responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), activeStudent.getId()))
                .thenReturn(true);
        when(noteRepository.findVisibleByStudentId(activeStudent.getId(), NoteStatus.OPEN)).thenReturn(List.of(visibleNote));

        List<NoteResponse> result = noteService.listByStudent(activeStudent.getId(), NoteStatus.OPEN, responsibleUser.getEmail());

        assertEquals(1, result.size());
        assertEquals(visibleNote.getId(), result.get(0).getId());
        verify(noteRepository).findVisibleByStudentId(activeStudent.getId(), NoteStatus.OPEN);
        verify(noteRepository, never()).findByStudentId(activeStudent.getId(), NoteStatus.OPEN);
    }

    @Test
    void directorHasFullAccessToListByStudent() {
        Note note = buildNote(902L, activeStudent, teacherAuthorUser, true, null);

        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));
        when(studentRepository.findById(activeStudent.getId())).thenReturn(Optional.of(activeStudent));
        when(noteRepository.findByStudentId(activeStudent.getId(), null)).thenReturn(List.of(note));

        List<NoteResponse> result = noteService.listByStudent(activeStudent.getId(), null, directorUser.getEmail());

        assertEquals(1, result.size());
        assertEquals(note.getId(), result.get(0).getId());
        verify(noteRepository).findByStudentId(activeStudent.getId(), null);
        verify(teacherRepository, never()).findByUserId(directorUser.getId());
        verify(responsibleRepository, never()).findByUserId(directorUser.getId());
    }

    @Test
    void directorCanUpdateAnyNote() {
        NoteUpdateRequest request = new NoteUpdateRequest();
        request.setContent("conteudo atualizado");
        request.setStatus(NoteStatus.RESOLVED);

        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));
        when(noteRepository.findById(noteFromAnotherTeacher.getId())).thenReturn(Optional.of(noteFromAnotherTeacher));
        when(noteRepository.save(noteFromAnotherTeacher)).thenReturn(noteFromAnotherTeacher);

        NoteResponse response = noteService.update(noteFromAnotherTeacher.getId(), request, directorUser.getEmail());

        assertEquals(noteFromAnotherTeacher.getId(), response.getId());
        assertEquals("conteudo atualizado", noteFromAnotherTeacher.getContent());
        assertEquals(NoteStatus.RESOLVED, noteFromAnotherTeacher.getStatus());
    }

    @Test
    void directorCanDeleteAnyNote() {
        when(userRepository.findByEmailAndDeletedAtIsNull(directorUser.getEmail())).thenReturn(Optional.of(directorUser));
        when(noteRepository.findById(noteFromAnotherTeacher.getId())).thenReturn(Optional.of(noteFromAnotherTeacher));
        when(noteRepository.save(noteFromAnotherTeacher)).thenReturn(noteFromAnotherTeacher);

        noteService.delete(noteFromAnotherTeacher.getId(), directorUser.getEmail());

        assertNotNull(noteFromAnotherTeacher.getDeletedAt());
        verify(noteRepository).save(noteFromAnotherTeacher);
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

    private static Note buildNote(Long id, Student student, User author, boolean visible, LocalDateTime deletedAt) {
        Note note = new Note();
        note.setId(id);
        note.setStudent(student);
        note.setAuthor(author);
        note.setTitle("Titulo");
        note.setContent("Conteudo");
        note.setType(NoteType.GENERAL);
        note.setStatus(NoteStatus.OPEN);
        note.setVisibleToResponsible(visible);
        note.setDeletedAt(deletedAt);
        return note;
    }
}
