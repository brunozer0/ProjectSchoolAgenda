package com.schoolagenda.dto.note;

import com.schoolagenda.domain.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteRequest {

    @NotNull
    private Long studentId;

    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private NoteType type;

    private boolean visibleToResponsible = true;
}