package com.schoolagenda.controller;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.dto.note.NoteMultipartRequest;
import com.schoolagenda.dto.note.NoteRequest;
import com.schoolagenda.dto.note.NoteReadResponse;
import com.schoolagenda.dto.note.NoteResponse;
import com.schoolagenda.dto.note.NoteUpdateRequest;
import com.schoolagenda.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Gestao de anotacoes dos alunos")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;

    @PostMapping(consumes = "multipart/form-data")
    @Operation(
            summary = "Criar anotacao",
            description = "Cria uma anotacao com possibilidade de upload de imagens"
    )
    public ResponseEntity<NoteResponse> create(
            @ModelAttribute @Valid NoteMultipartRequest form,
            Authentication auth) {

        NoteRequest request = new NoteRequest();
        request.setStudentId(form.getStudentId());
        request.setTitle(form.getTitle());
        request.setContent(form.getContent());
        request.setType(form.getType());
        request.setVisibleToResponsible(form.isVisibleToResponsible());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.create(request, form.getImages(), auth.getName()));
    }


    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar anotacao", description = "Atualiza campos parciais de uma anotacao.")
    public ResponseEntity<NoteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NoteUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(noteService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    @Operation(summary = "Excluir anotacao", description = "Realiza soft delete da anotacao.")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication auth) {
        noteService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'RESPONSIBLE')")
    @Operation(summary = "Consultar anotacao", description = "Consulta sem marcar como lida.")
    public ResponseEntity<NoteResponse> findById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(noteService.findById(id, auth.getName()));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'RESPONSIBLE')")
    @Operation(summary = "Marcar anotacao como lida", description = "Registra a primeira leitura do usuario autenticado de forma idempotente.")
    public ResponseEntity<NoteReadResponse> markAsRead(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(noteService.markAsRead(id, auth.getName()));
    }

    @GetMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'RESPONSIBLE')")
    @Operation(summary = "Consultar minha leitura", description = "Consulta a leitura sem modifica-la.")
    public ResponseEntity<NoteReadResponse> getReadStatus(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(noteService.getReadStatus(id, auth.getName()));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'RESPONSIBLE')")
    @Operation(summary = "Listar anotacoes por aluno", description = "Lista anotacoes por aluno com filtro opcional de status.")
    public ResponseEntity<List<NoteResponse>> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) NoteStatus status,
            Authentication auth) {
        return ResponseEntity.ok(noteService.listByStudent(studentId, status, auth.getName()));
    }
}
