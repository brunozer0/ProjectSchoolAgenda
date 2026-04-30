package com.schoolagenda.service;

import com.schoolagenda.domain.entity.*;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.note.NoteRequest;
import com.schoolagenda.dto.note.NoteResponse;
import com.schoolagenda.dto.note.NoteUpdateRequest;
import com.schoolagenda.exception.ForbiddenException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ResponsibleRepository responsibleRepository;
    private final ResponsibleStudentRepository responsibleStudentRepository;
    private final UserRepository userRepository;

    public NoteResponse create(NoteRequest request, String authorEmail) {
        User author = getActiveUser(authorEmail);
        Student student = getActiveStudent(request.getStudentId());

        RoleName role = author.getRole().getName();

        if (role == RoleName.RESPONSIBLE) {
            throw new ForbiddenException("Responsaveis nao podem criar anotacoes.");
        }

        // Teacher can only create notes for students in their own classroom.
        if (role == RoleName.TEACHER) {
            assertTeacherOwnsStudent(author.getId(), student.getId());
        }

        Note note = new Note();
        note.setStudent(student);
        note.setAuthor(author);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setType(request.getType());
        note.setVisibleToResponsible(request.isVisibleToResponsible());

        return NoteResponse.from(noteRepository.save(note));
    }

    public NoteResponse update(Long noteId, NoteUpdateRequest request, String authorEmail) {
        User author = getActiveUser(authorEmail);
        Note note = getActiveNote(noteId);

        RoleName role = author.getRole().getName();

        if (role == RoleName.RESPONSIBLE) {
            throw new ForbiddenException("Responsaveis nao podem editar anotacoes.");
        }

        // Teacher can only edit notes created by themselves.
        if (role == RoleName.TEACHER && !note.getAuthor().getId().equals(author.getId())) {
            throw new ForbiddenException("Professores so podem editar as proprias anotacoes.");
        }

        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getType() != null) {
            note.setType(request.getType());
        }
        if (request.getVisibleToResponsible() != null) {
            note.setVisibleToResponsible(request.getVisibleToResponsible());
        }
        if (request.getStatus() != null) {
            note.setStatus(request.getStatus());
        }

        return NoteResponse.from(noteRepository.save(note));
    }

    public void delete(Long noteId, String authorEmail) {
        User author = getActiveUser(authorEmail);
        Note note = getActiveNote(noteId);

        RoleName role = author.getRole().getName();

        if (role == RoleName.RESPONSIBLE) {
            throw new ForbiddenException("Responsaveis nao podem excluir anotacoes.");
        }

        // Teacher can only delete notes created by themselves.
        if (role == RoleName.TEACHER && !note.getAuthor().getId().equals(author.getId())) {
            throw new ForbiddenException("Professores so podem excluir as proprias anotacoes.");
        }

        note.setDeletedAt(LocalDateTime.now());
        noteRepository.save(note);
    }

    public List<NoteResponse> listByStudent(Long studentId, NoteStatus status, String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        RoleName role = requester.getRole().getName();
        // Keep behavior consistent: listing should fail with 404 when student does not exist/is deleted.
        Student student = getActiveStudent(studentId);

        if (role == RoleName.RESPONSIBLE) {
            Responsible responsible = responsibleRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado"));

            // Responsible can only access notes of linked students.
            if (!responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), student.getId())) {
                throw new ForbiddenException("Acesso negado as anotacoes deste aluno.");
            }

            return noteRepository.findVisibleByStudentId(student.getId(), status)
                    .stream()
                    .map(NoteResponse::from)
                    .toList();
        }

        // Teacher can only access notes from their own students.
        if (role == RoleName.TEACHER) {
            assertTeacherOwnsStudent(requester.getId(), student.getId());
        }

        return noteRepository.findByStudentId(student.getId(), status)
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    private void assertTeacherOwnsStudent(Long teacherUserId, Long studentId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        if (!studentRepository.existsByStudentAndTeacher(
                studentId, teacher.getId())) {
            throw new ForbiddenException("O aluno nao pertence a sua turma.");
        }
    }

    private User getActiveUser(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }

    private Student getActiveStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno nao encontrado"));

        if (student.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Aluno nao encontrado");
        }

        return student;
    }

    private Note getActiveNote(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anotacao nao encontrada"));

        if (note.isDeleted()) {
            throw new ResourceNotFoundException("Anotacao nao encontrada");
        }

        return note;
    }
}
