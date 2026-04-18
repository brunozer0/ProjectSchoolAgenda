package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.domain.entity.Responsible;
import com.schoolagenda.domain.entity.ResponsibleStudent;
import com.schoolagenda.domain.entity.Role;
import com.schoolagenda.domain.entity.Student;
import com.schoolagenda.domain.entity.User;
import com.schoolagenda.domain.enums.RoleName;
import com.schoolagenda.dto.director.FamilyChildRequest;
import com.schoolagenda.dto.director.FamilyCreateRequest;
import com.schoolagenda.dto.director.FamilyCreateResponse;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import com.schoolagenda.repository.ResponsibleRepository;
import com.schoolagenda.repository.ResponsibleStudentRepository;
import com.schoolagenda.repository.RoleRepository;
import com.schoolagenda.repository.StudentRepository;
import com.schoolagenda.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorFamilyService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ResponsibleRepository responsibleRepository;
    private final StudentRepository studentRepository;
    private final ResponsibleStudentRepository responsibleStudentRepository;
    private final ClassroomRepository classroomRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public FamilyCreateResponse createFamily(FamilyCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("E-mail ja cadastrado.");
        }

        Role role = roleRepository.findByName(RoleName.RESPONSIBLE)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil RESPONSIBLE nao encontrado"));

        User user = new User();
        user.setName(request.getFirstName().trim() + " " + request.getLastName().trim());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        User savedUser = userRepository.save(user);

        Responsible responsible = new Responsible();
        responsible.setUser(savedUser);
        Responsible savedResponsible = responsibleRepository.save(responsible);

        List<Long> createdStudentIds = new ArrayList<>();
        for (FamilyChildRequest child : request.getChildren()) {
            Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(child.getClassroomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turma nao encontrada"));

            Student student = new Student();
            student.setName(child.getName());
            student.setGender(child.getGender());
            student.setBirthDate(child.getBirthDate());
            student.setClassroom(classroom);
            Student savedStudent = studentRepository.save(student);

            ResponsibleStudent link = new ResponsibleStudent();
            link.setResponsible(savedResponsible);
            link.setStudent(savedStudent);
            responsibleStudentRepository.save(link);
            createdStudentIds.add(savedStudent.getId());
        }

        return FamilyCreateResponse.builder()
                .responsibleId(savedResponsible.getId())
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .studentIds(createdStudentIds)
                .build();
    }
}
