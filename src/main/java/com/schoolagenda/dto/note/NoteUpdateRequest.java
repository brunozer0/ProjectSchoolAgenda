package com.schoolagenda.dto.note;

import com.schoolagenda.domain.enums.NoteStatus;
import com.schoolagenda.domain.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteUpdateRequest {

    @Size(max = 200)
    private String title;

    private String content;

    private NoteType type;

    private Boolean visibleToResponsible;

    private NoteStatus status;
}