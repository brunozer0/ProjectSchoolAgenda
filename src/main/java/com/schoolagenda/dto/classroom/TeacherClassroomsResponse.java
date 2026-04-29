package com.schoolagenda.dto.classroom;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Teacher;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class TeacherClassroomsResponse {

    private Long teacherId;
    private String teacherName;
    private List<ClassroomResponse> classrooms;

    public static TeacherClassroomsResponse from(Teacher teacher) {
        return TeacherClassroomsResponse.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getUser().getName())
                .classrooms(
                        teacher.getClassrooms()
                                .stream()
                                .filter(c -> c.getDeletedAt() == null)
                                .map(ClassroomResponse::from)
                                .toList()
                )
                .build();
    }

}