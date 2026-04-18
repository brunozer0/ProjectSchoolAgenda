package com.schoolagenda.controller;

import com.schoolagenda.dto.student.StudentResponse;
import com.schoolagenda.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Consulta de alunos por perfil")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/my/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Listar alunos do professor logado")
    public ResponseEntity<List<StudentResponse>> listMyStudentsAsTeacher(Authentication auth) {
        return ResponseEntity.ok(studentService.listMyStudentsAsTeacher(auth.getName()));
    }

    @GetMapping("/my/responsible")
    @PreAuthorize("hasRole('RESPONSIBLE')")
    @Operation(summary = "Listar alunos do responsavel logado")
    public ResponseEntity<List<StudentResponse>> listMyStudentsAsResponsible(Authentication auth) {
        return ResponseEntity.ok(studentService.listMyStudentsAsResponsible(auth.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DIRECTOR', 'TEACHER', 'RESPONSIBLE')")
    @Operation(summary = "Buscar aluno por id")
    public ResponseEntity<StudentResponse> findById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(studentService.findById(id, auth.getName()));
    }
}
