package com.schoolagenda.dto.director;

import com.schoolagenda.dto.classroom.ClassroomResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeacherListResponse {
    private Long teacherId;
    private Long userId;
    private String name;
    private String email;
    private List<ClassroomResponse> classrooms;
}
