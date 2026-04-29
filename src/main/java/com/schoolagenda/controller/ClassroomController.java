package com.schoolagenda.controller;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.dto.classroom.ClassroomStudentsResponse;
import com.schoolagenda.dto.classroom.TeacherClassroomsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
@Tag(name = "Classrooms", description = "Gestão de salas")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor

public class ClassroomController {

    private final com.schoolagenda.service.ClassroomService classroomService;

    @GetMapping("/my/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Listar turmas do professor logado",
            description = "Retorna todas as turmas associadas ao professor autenticado."
    )
    public ResponseEntity<TeacherClassroomsResponse> getMyClassrooms(Authentication auth) {

        return ResponseEntity.ok(
                classroomService.getMyClassrooms(auth.getName())
        );
    }


    @GetMapping("/{classroomId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Listar alunos de uma turma",
            description = "Retorna todos os alunos associados à turma informada pelo ID. O acesso é restrito a professores e deve pertencer às suas turmas."
    )
    public ResponseEntity<ClassroomStudentsResponse> getStudents(
            @PathVariable Long classroomId,
            Authentication auth) {

        return ResponseEntity.ok(
                classroomService.getStudentsFromClassroom(classroomId, auth.getName())
        );
    }
}