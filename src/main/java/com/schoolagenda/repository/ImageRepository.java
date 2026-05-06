package com.schoolagenda.repository;

import com.schoolagenda.domain.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByNoteId(Long noteId);
}
