package com.schoolagenda.dto.student;

import com.schoolagenda.domain.entity.Student;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class StudentResponse {

    private Long id;
    private String name;
    private LocalDate birthDate;
    private Long classroomId;
    private String classroomName;

    public static StudentResponse from(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .birthDate(student.getBirthDate())
                .classroomId(student.getClassroom() != null ? student.getClassroom().getId() : null)
                .classroomName(student.getClassroom() != null ? student.getClassroom().getName() : null)
                .build();
    }
}