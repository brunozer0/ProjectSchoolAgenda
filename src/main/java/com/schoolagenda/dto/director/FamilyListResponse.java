package com.schoolagenda.dto.director;

import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.dto.student.StudentResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder

public class FamilyListResponse {

    private Long responsibleId;
    private Long userId;
    private String name;
    private String email;
    private List<StudentResponse> students;


    public static FamilyListResponse from(Responsible responsible) {
        return FamilyListResponse.builder()
                .responsibleId(responsible.getId())
                .userId(responsible.getUser().getId())
                .name(responsible.getUser().getName())
                .email(responsible.getUser().getEmail())
                .students(
                        responsible.getResponsibleStudents()
                                .stream()
                                .map(link -> StudentResponse.from(link.getStudent()))
                                .toList()
                )
                .build();
    }
}
