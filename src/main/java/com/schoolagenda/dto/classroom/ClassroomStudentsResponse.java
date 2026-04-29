package com.schoolagenda.dto.classroom;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.dto.director.TeacherResponse;
import com.schoolagenda.dto.student.StudentResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ClassroomStudentsResponse {

    private ClassroomResponse classroom;
    private List<TeacherResponse> teachers;
    private List<StudentResponse> students;

    public static ClassroomStudentsResponse from(Classroom classroom, List<Student> students) {
        return ClassroomStudentsResponse.builder()
                .classroom(ClassroomResponse.from(classroom))
                .teachers(
                        classroom.getTeachers().stream()
                                .map(TeacherResponse::from)
                                .toList()
                )
                .students(
                        students.stream()
                                .filter(s -> s.getDeletedAt() == null)
                                .map(StudentResponse::from)
                                .toList()
                )
                .build();
    }
}
