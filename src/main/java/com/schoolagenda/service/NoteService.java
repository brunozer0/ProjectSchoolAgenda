package com.schoolagenda.service;

import com.schoolagenda.domain.entity.*;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.note.NoteRequest;
import com.schoolagenda.dto.note.NoteReadResponse;
import com.schoolagenda.dto.note.NoteResponse;
import com.schoolagenda.dto.note.NoteUpdateRequest;
import com.schoolagenda.exception.BadRequestException;
import com.schoolagenda.exception.ForbiddenException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.infrastructure.storage.StorageService;
import com.schoolagenda.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ResponsibleRepository responsibleRepository;
    private final ResponsibleStudentRepository responsibleStudentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ImageRepository imageRepository;
    private final NoteReadRepository noteReadRepository;

    @Value("${gcp.storage.bucket}")
    private String bucketName;

    public NoteResponse create(NoteRequest request,
                               List<MultipartFile> files,
                               String authorEmail) {

        User author = getActiveUser(authorEmail);
        Student student = getActiveStudent(request.getStudentId());

        RoleName role = author.getRole().getName();

        if (role == RoleName.TEACHER) {
            assertTeacherOwnsStudent(author.getId(), student.getId());
        }

        if (role == RoleName.RESPONSIBLE) {
            assertResponsibleOwnsStudent(author.getId(), student.getId());
        }

        validateFiles(files);

        Note note = new Note();
        note.setStudent(student);
        note.setAuthor(author);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setType(request.getType());
        note.setVisibleToResponsible(request.isVisibleToResponsible());

        List<String> uploadedKeys = new ArrayList<>();
        boolean synchronizedTransaction = TransactionSynchronizationManager.isSynchronizationActive();
        if (synchronizedTransaction) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) cleanupUploads(uploadedKeys);
                    else if (status == STATUS_UNKNOWN && !uploadedKeys.isEmpty()) {
                        log.error("Resultado da transacao desconhecido; verificar arquivos {}", uploadedKeys);
                    }
                }
            });
        }
        try {
            note = noteRepository.save(note);
            note.setImages(new ArrayList<>());
            if (files != null && !files.isEmpty()) {
                processFiles(note, files, uploadedKeys);
            }
            return NoteResponse.from(note, bucketName);
        } catch (RuntimeException exception) {
            if (!synchronizedTransaction) cleanupUploads(uploadedKeys);
            throw exception;
        }
    }
    public NoteResponse update(Long noteId, NoteUpdateRequest request, String authorEmail) {
        User author = getActiveUser(authorEmail);
        Note note = getActiveNote(noteId);

        RoleName role = author.getRole().getName();

        assertCanAccessNote(author, note);
        boolean editsContent = request.getTitle() != null || request.getContent() != null
                || request.getType() != null || request.getVisibleToResponsible() != null;
        if (role != RoleName.DIRECTOR && editsContent
                && !note.getAuthor().getId().equals(author.getId())) {
            throw new ForbiddenException("Somente o autor ou a direcao podem editar o conteudo da anotacao.");
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

        return NoteResponse.from(noteRepository.save(note), bucketName);
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

        assertCanAccessNote(author, note);
        List<String> keys = note.getImages() == null ? List.of()
                : note.getImages().stream().map(Image::getStorageKey).toList();
        if (note.getImages() != null && !note.getImages().isEmpty()) {
            imageRepository.deleteAll(note.getImages());
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { cleanupUploads(keys); }
            });
        } else {
            cleanupUploads(keys);
        }

        note.setDeletedAt(LocalDateTime.now());
        noteRepository.save(note);
    }

    @Transactional(readOnly = true)
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

            return noteRepository.findVisibleByStudentIdWithImages(student.getId(), status)
                    .stream()
                    .map(note -> NoteResponse.from(note, bucketName))
                    .toList();
        }

        // Teacher can only access notes from their own students.
        if (role == RoleName.TEACHER) {
            assertTeacherOwnsStudent(requester.getId(), student.getId());
        }

        return noteRepository.findByStudentIdWithImages(student.getId(), status)
                .stream()
                .map(note -> NoteResponse.from(note, bucketName))
                .toList();
    }



    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        if (files.size() > 5) {
            throw new BadRequestException("Maximo de 5 arquivos permitido");
        }

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                throw new BadRequestException("Arquivo vazio");
            }

            String type = file.getContentType();

            if (type == null ||
                    (!type.startsWith("image/") && !type.equals("application/pdf"))) {
                throw new BadRequestException("Apenas imagens e PDF sao permitidos");
            }

            if (file.getSize() > 5_000_000) {
                throw new BadRequestException("Arquivo muito grande (max 5MB)");
            }
        }
    }

    private void processFiles(Note note, List<MultipartFile> files, List<String> uploadedKeys) {
        for (MultipartFile file : files) {
            try {
                String originalName = file.getOriginalFilename();

                String extension = "";

                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf("."));
                }

                String key = UUID.randomUUID() + extension;

                String storageKey;
                try (var content = file.getInputStream()) {
                    storageKey = storageService.upload(key, content, file.getContentType());
                    uploadedKeys.add(storageKey);
                }

                Image image = new Image();
                image.setNote(note);
                image.setFilename(originalName);
                image.setStorageKey(storageKey);
                image.setContentType(file.getContentType());
                image.setSize(file.getSize());

                note.getImages().add(imageRepository.save(image));

            } catch (Exception e) {
                throw new RuntimeException("Erro ao fazer upload do arquivo", e);
            }
        }
    }

    private void assertTeacherOwnsStudent(Long teacherUserId, Long studentId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        if (!studentRepository.existsByStudentAndTeacher(
                studentId, teacher.getId())) {
            throw new ForbiddenException("O aluno nao pertence a sua turma.");
        }
    }

    private void assertResponsibleOwnsStudent(Long userId, Long studentId) {
        Responsible responsible = responsibleRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsavel nao encontrado"));

        if (!responsibleStudentRepository.existsByResponsibleIdAndStudentId(
                responsible.getId(), studentId)) {
            throw new ForbiddenException("O aluno nao pertence ao responsavel.");
        }
    }

    private void cleanupUploads(List<String> keys) {
        for (String key : keys) {
            try {
                storageService.delete(key);
            } catch (RuntimeException exception) {
                log.error("Falha ao limpar arquivo {}", key, exception);
            }
        }
    }

    private void assertCanAccessNote(User requester, Note note) {
        switch (requester.getRole().getName()) {
            case DIRECTOR -> { }
            case TEACHER -> assertTeacherOwnsStudent(requester.getId(), note.getStudent().getId());
            case RESPONSIBLE -> {
                assertResponsibleOwnsStudent(requester.getId(), note.getStudent().getId());
                if (!note.isVisibleToResponsible()) {
                    throw new ForbiddenException("Anotacao nao disponivel para responsaveis.");
                }
            }
            default -> throw new ForbiddenException("Perfil sem permissao para acessar anotacoes.");
        }
    }

    @Transactional(readOnly = true)
    public NoteResponse findById(Long noteId, String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        Note note = getActiveNote(noteId);
        assertCanAccessNote(requester, note);
        return NoteResponse.from(note, bucketName);
    }

    public NoteReadResponse markAsRead(Long noteId, String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        Note note = getActiveNote(noteId);
        assertCanAccessNote(requester, note);
        LocalDateTime readAt = noteReadRepository.markAsRead(note.getId(), requester.getId());
        return new NoteReadResponse(note.getId(), true, readAt);
    }

    @Transactional(readOnly = true)
    public NoteReadResponse getReadStatus(Long noteId, String requesterEmail) {
        User requester = getActiveUser(requesterEmail);
        Note note = getActiveNote(noteId);
        assertCanAccessNote(requester, note);
        var readAt = noteReadRepository.findReadAt(note.getId(), requester.getId());
        return new NoteReadResponse(note.getId(), readAt.isPresent(), readAt.orElse(null));
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

        if (note.isDeleted() || note.getStudent().isDeleted()) {
            throw new ResourceNotFoundException("Anotacao nao encontrada");
        }

        return note;
    }
}
