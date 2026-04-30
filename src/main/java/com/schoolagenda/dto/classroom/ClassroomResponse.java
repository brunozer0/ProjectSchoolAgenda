package com.schoolagenda.dto.classroom;

import com.schoolagenda.domain.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@Schema(description = "Resposta de criação de sala")

public class ClassroomResponse {

    private Long id;
    private String name;

    public static ClassroomResponse from(Classroom classroom) {
        return ClassroomResponse.builder()
                .id(classroom.getId())
                .name(classroom.getName())
                .build();
    }
}