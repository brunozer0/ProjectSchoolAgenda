package com.schoolagenda.controller;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.dto.note.NoteRequest;
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

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Gestao de anotacoes dos alunos")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
    @Operation(summary = "Criar anotacao", description = "Permite criacao de anotacao por DIRECTOR ou TEACHER.")
    public ResponseEntity<NoteResponse> create(
            @Valid @RequestBody NoteRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.create(request, auth.getName()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER')")
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
