package com.schoolagenda.dto.note;

import com.schoolagenda.domain.enums.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class NoteMultipartRequest {

    @NotNull
    private Long studentId;

    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private NoteType type;

    private boolean visibleToResponsible = true;

    private List<MultipartFile> images;
}