package com.schoolagenda.controller;

import com.schoolagenda.dto.classroom.ClassroomResponse;
import com.schoolagenda.dto.director.*;
import com.schoolagenda.service.DirectorClassroomService;
import com.schoolagenda.service.DirectorFamilyService;
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
    private final DirectorClassroomService directorClassroomService;



//    public
    @GetMapping("/families")
    @Operation(summary = "Listar familias", description = "Lista familias, com seus respectivos responsaveis e filhos.")
    public ResponseEntity<List<FamilyListResponse>> listFamilies () {
        return ResponseEntity.ok().body(directorFamilyService.getFamilies());
    }

    @PostMapping("/families")
    @Operation(summary = "Cadastrar familia", description = "Cria responsavel, cria alunos e realiza vinculos automaticamente.")
    public ResponseEntity<FamilyCreateResponse> createFamily(@Valid @RequestBody FamilyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directorFamilyService.createFamily(request));
    }

    @DeleteMapping("/families/{responsibleId}")
    @Operation(
            summary = "Deletar familia",
            description = "Realiza soft delete do usuário e alunos e remove vínculos do responsável."
    )
    public ResponseEntity<Void> deleteFamily(@PathVariable long responsibleId) {
        directorFamilyService.deleteFamily(responsibleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teachers")
    @Operation(summary = "Cadastrar professor", description = "Cria professor e turmas vinculadas.")
    public ResponseEntity<TeacherCreateResponse> createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directorTeacherService.createTeacher(request));
    }

    @PostMapping("/classrooms")
    @Operation(summary = "Cadastrar sala", description = "Cria uma sala de aula.")
    public ResponseEntity<ClassroomResponse> createClassroom(
            @Valid @RequestBody ClassroomCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(directorClassroomService.createClassroom(request));
    }

    @GetMapping("/classrooms")
    @Operation(summary = "Listar salas", description = "Lista todas as salas ativas.")
    public ResponseEntity<List<ClassroomResponse>> listClassrooms() {
        return ResponseEntity.ok(directorClassroomService.listClassrooms());
    }

    @DeleteMapping("/classrooms/{id}")
    @Operation(summary = "Deletar sala", description = "Remove logicamente a sala (soft delete).")
    public ResponseEntity<Void> deleteClassroom(@PathVariable Long id) {
        directorClassroomService.deleteClassroom(id);
        return ResponseEntity.noContent().build();
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
