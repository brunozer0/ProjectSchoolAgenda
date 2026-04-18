package com.schoolagenda.controller;

import com.schoolagenda.dto.director.FamilyCreateRequest;
import com.schoolagenda.dto.director.FamilyCreateResponse;
import com.schoolagenda.dto.director.TeacherListResponse;
import com.schoolagenda.service.DirectorFamilyService;
import com.schoolagenda.dto.director.TeacherCreateRequest;
import com.schoolagenda.dto.director.TeacherCreateResponse;
import com.schoolagenda.service.DirectorTeacherService;
import com.schoolagenda.service.ResponsibleStudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/director")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DIRECTOR')")
@Tag(name = "Director", description = "Operacoes administrativas do diretor")
@SecurityRequirement(name = "bearerAuth")
public class DirectorController {

    private final ResponsibleStudentService responsibleStudentService;
    private final DirectorFamilyService directorFamilyService;
    private final DirectorTeacherService directorTeacherService;

    @PostMapping("/families")
    @Operation(summary = "Cadastrar familia", description = "Cria responsavel, cria alunos e realiza vinculos automaticamente.")
    public ResponseEntity<FamilyCreateResponse> createFamily(@Valid @RequestBody FamilyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directorFamilyService.createFamily(request));
    }

    @PostMapping("/teachers")
    @Operation(summary = "Cadastrar professor", description = "Cria professor e turmas vinculadas.")
    public ResponseEntity<TeacherCreateResponse> createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directorTeacherService.createTeacher(request));
    }

    @GetMapping("/teachers")
    @Operation(summary = "Listar professores", description = "Lista professores ativos com suas turmas ativas.")
    public ResponseEntity<List<TeacherListResponse>> listTeachers() {
        return ResponseEntity.ok(directorTeacherService.listTeachers());
    }

    @DeleteMapping("/teachers/{teacherId}")
    @Operation(summary = "Excluir professor", description = "Realiza soft delete do professor e de suas turmas.")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long teacherId) {
        directorTeacherService.deleteTeacher(teacherId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/responsibles/{responsibleId}/students/{studentId}")
    @Operation(summary = "Vincular responsavel ao aluno")
    public ResponseEntity<Void> linkStudent(
            @PathVariable Long responsibleId,
            @PathVariable Long studentId) {
        responsibleStudentService.linkResponsibleToStudent(responsibleId, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/responsibles/{responsibleId}/students/{studentId}")
    @Operation(summary = "Desvincular responsavel do aluno")
    public ResponseEntity<Void> unlinkStudent(
            @PathVariable Long responsibleId,
            @PathVariable Long studentId) {
        responsibleStudentService.unlinkResponsibleFromStudent(responsibleId, studentId);
        return ResponseEntity.noContent().build();
    }
}
