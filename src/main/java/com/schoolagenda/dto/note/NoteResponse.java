package com.schoolagenda.dto.note;

import com.schoolagenda.domain.entity.Note;
import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.domain.enums.NoteType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    public static NoteResponse from(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .studentId(note.getStudent().getId())
                .studentName(note.getStudent().getName())
                .authorId(note.getAuthor().getId())
                .authorName(note.getAuthor().getName())
                .title(note.getTitle())
                .content(note.getContent())
                .type(note.getType())
                .status(note.getStatus())
                .visibleToResponsible(note.isVisibleToResponsible())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}