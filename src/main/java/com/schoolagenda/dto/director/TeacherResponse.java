package com.schoolagenda.dto.director;

import com.schoolagenda.domain.entity.Teacher;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherResponse {
    private Long id;
    private String name;

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getUser().getName()
        );
    }
}
