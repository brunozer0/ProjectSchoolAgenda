package com.schoolagenda.dto.note;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolagenda.domain.entity.Note;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.domain.enums.NoteType;
import com.schoolagenda.dto.image.ImageResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
public class NoteResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long authorId;
    private String authorName;
    private String title;
    private String content;
    private NoteType type;
    private NoteStatus status;
    private boolean visibleToResponsible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<ImageResponse> images = List.of();
    private String authorRole;

    public static NoteResponse from(Note note, String bucketName) {

        List<ImageResponse> imageResponses =
                Optional.ofNullable(note.getImages())
                        .orElse(List.of())
                        .stream()
                        .map(img -> ImageResponse.from(img, bucketName))
                        .toList();
        return NoteResponse.builder()
                .id(note.getId())
                .studentId(note.getStudent().getId())
                .studentName(note.getStudent().getName())
                .authorId(note.getAuthor().getId())
                .authorName(note.getAuthor().getName())
                .authorRole(note.getAuthor().getRole().getName().name())
                .title(note.getTitle())
                .content(note.getContent())
                .type(note.getType())
                .status(note.getStatus())
                .visibleToResponsible(note.isVisibleToResponsible())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .images(imageResponses)
                .build();
    }
}