package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.Teacher;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.classroom.ClassroomResponse;
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
import java.util.Set;
import java.util.stream.Collectors;

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

        List<Long> requestedIds = request.getClassroomIds();

        List<Classroom> classrooms =
                classroomRepository.findAllByIdInAndDeletedAtIsNull(requestedIds);


        if (classrooms.size() != requestedIds.size()) {

            Set<Long> foundIds = classrooms.stream()
                    .map(Classroom::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = requestedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new ResourceNotFoundException(
                    "Classrooms nao encontradas: " + missingIds
            );
        }


        for (Classroom classroom : classrooms) {
            classroom.getTeachers().add(savedTeacher);
        }

        classroomRepository.saveAll(classrooms);

        return TeacherCreateResponse.builder()
                .teacherId(savedTeacher.getId())
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .classroomIds(
                        classrooms.stream().map(Classroom::getId).toList()
                )
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeacherListResponse> listTeachers() {

        return teacherRepository.findAllWithClassrooms()
                .stream()
                .map(teacher -> {

                    List<ClassroomResponse> classrooms = teacher.getClassrooms().stream()
                            .filter(c -> c.getDeletedAt() == null)
                            .map(ClassroomResponse::from)
                            .toList();

                    return TeacherListResponse.builder()
                            .teacherId(teacher.getId())
                            .userId(teacher.getUser().getId())
                            .name(teacher.getUser().getName())
                            .email(teacher.getUser().getEmail())
                            .classrooms(classrooms)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void deleteTeacher(Long teacherId) {

        Teacher teacher = teacherRepository.findActiveById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor nao encontrado"));

        LocalDateTime now = LocalDateTime.now();

        User user = teacher.getUser();
        user.setDeletedAt(now);

        List<Classroom> classrooms = classroomRepository
                .findByTeachers_IdAndDeletedAtIsNull(teacherId);

        for (Classroom classroom : classrooms) {
            classroom.getTeachers().remove(teacher);
        }

        classroomRepository.saveAll(classrooms);
    }
}
