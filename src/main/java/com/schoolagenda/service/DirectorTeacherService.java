package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.director.TeacherListResponse;
import com.schoolagenda.dto.director.TeacherCreateRequest;
import com.schoolagenda.dto.director.TeacherCreateResponse;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import com.schoolagenda.repository.RoleRepository;
import com.schoolagenda.repository.TeacherRepository;
import com.schoolagenda.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorTeacherService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TeacherCreateResponse createTeacher(TeacherCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("E-mail ja cadastrado.");
        }

        Role role = roleRepository.findByName(RoleName.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil TEACHER nao encontrado"));

        User user = new User();
        user.setName(request.getFirstName().trim() + " " + request.getLastName().trim());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        User savedUser = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(savedUser);
        Teacher savedTeacher = teacherRepository.save(teacher);

        List<Long> classroomIds = new ArrayList<>();
        for (String classroomName : request.getClassrooms()) {
            Classroom classroom = new Classroom();
            classroom.setName(classroomName.trim());
            classroom.setTeacher(savedTeacher);
            Classroom savedClassroom = classroomRepository.save(classroom);
            classroomIds.add(savedClassroom.getId());
        }

        return TeacherCreateResponse.builder()
                .teacherId(savedTeacher.getId())
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .classroomIds(classroomIds)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeacherListResponse> listTeachers() {
        return teacherRepository.findAllByUserDeletedAtIsNullOrderByUserNameAsc()
                .stream()
                .map(teacher -> {
                    List<String> classroomNames = classroomRepository
                            .findAllByTeacherIdAndDeletedAtIsNull(teacher.getId())
                            .stream()
                            .map(Classroom::getName)
                            .toList();

                    return TeacherListResponse.builder()
                            .teacherId(teacher.getId())
                            .userId(teacher.getUser().getId())
                            .name(teacher.getUser().getName())
                            .email(teacher.getUser().getEmail())
                            .classrooms(classroomNames)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void deleteTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        User user = userRepository.findByIdAndDeletedAtIsNull(teacher.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        LocalDateTime now = LocalDateTime.now();
        user.setDeletedAt(now);
        userRepository.save(user);

        List<Classroom> classrooms = classroomRepository.findAllByTeacherIdAndDeletedAtIsNull(teacherId);
        for (Classroom classroom : classrooms) {
            classroom.setDeletedAt(now);
        }
        classroomRepository.saveAll(classrooms);
    }
}
