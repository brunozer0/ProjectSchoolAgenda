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
            throw new ForbiddenException("Responsibles cannot create notes.");
        }

        if (role == RoleName.TEACHER) {
            assertTeacherOwnsStudent(author.getId(), student.getId());
        }

        Note note = new Note();
        note.setStudent(student);
        note.setAuthor(author);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setType(request.getType());
        note.setVisibleToResponsible(request.isVisibleToResponsible()); // troque para getVisibleToResponsible() se seu DTO foi ajustado

        return NoteResponse.from(noteRepository.save(note));
    }

    public NoteResponse update(Long noteId, NoteUpdateRequest request, String authorEmail) {
        User author = getActiveUser(authorEmail);
        Note note = getActiveNote(noteId);

        RoleName role = author.getRole().getName();

        if (role == RoleName.RESPONSIBLE) {
            throw new ForbiddenException("Responsibles cannot edit notes.");
        }

        if (role == RoleName.TEACHER && !note.getAuthor().getId().equals(author.getId())) {
            throw new ForbiddenException("Teachers can only edit their own notes.");
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
            throw new ForbiddenException("Responsibles cannot delete notes.");
        }

        if (role == RoleName.TEACHER && !note.getAuthor().getId().equals(author.getId())) {
            throw new ForbiddenException("Teachers can only delete their own notes.");
        }

        note.setDeletedAt(LocalDateTime.now());
        noteRepository.save(note);
    }

    public List<NoteResponse> listByStudent(Long studentId, NoteStatus status, String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        RoleName role = requester.getRole().getName();

        if (role == RoleName.RESPONSIBLE) {
            Responsible responsible = responsibleRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Responsible not found"));

            if (!responsibleStudentRepository.existsByResponsibleIdAndStudentId(responsible.getId(), studentId)) {
                throw new ForbiddenException("Access denied to this student's notes.");
            }

            return noteRepository.findVisibleByStudentId(studentId, status)
                    .stream()
                    .map(NoteResponse::from)
                    .toList();
        }

        if (role == RoleName.TEACHER) {
            assertTeacherOwnsStudent(requester.getId(), studentId);
        }

        return noteRepository.findByStudentId(studentId, status)
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    private void assertTeacherOwnsStudent(Long teacherUserId, Long studentId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        if (!studentRepository.existsByIdAndClassroomTeacherIdAndDeletedAtIsNull(studentId, teacher.getId())) {
            throw new ForbiddenException("Student does not belong to your classroom.");
        }
    }

    private User getActiveUser(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Student getActiveStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (student.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Student not found");
        }

        return student;
    }

    private Note getActiveNote(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (note.isDeleted()) {
            throw new ResourceNotFoundException("Note not found");
        }

        return note;
    }
}