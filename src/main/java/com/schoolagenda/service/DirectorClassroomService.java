package com.schoolagenda.service;

import com.schoolagenda.domain.entity.Classroom;
import com.schoolagenda.dto.classroom.ClassroomResponse;
import com.schoolagenda.dto.director.ClassroomCreateRequest;
import com.schoolagenda.exception.ConflictException;
import com.schoolagenda.exception.ResourceNotFoundException;
import com.schoolagenda.repository.ClassroomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DirectorClassroomService {

    private final ClassroomRepository repository;

    @Transactional
    public ClassroomResponse createClassroom(ClassroomCreateRequest request) {

        if (repository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new ConflictException("Já existe uma sala com esse nome.");
        }

        Classroom classroom = new Classroom();
        classroom.setName(request.getName());

        Classroom saved = repository.save(classroom);

        return ClassroomResponse.from(saved);
    }

    public List<ClassroomResponse> listClassrooms() {

        return repository.findAllByDeletedAtIsNull()
                .stream()
                .map(classroom -> ClassroomResponse.builder()
                        .id(classroom.getId())
                        .name(classroom.getName())
                        .build()
                )
                .toList();

    }

    @Transactional
    public void deleteClassroom(Long id) {

        Classroom classroom = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala não encontrada."));

        classroom.setDeletedAt(LocalDateTime.now());

        repository.save(classroom);
    }



}
