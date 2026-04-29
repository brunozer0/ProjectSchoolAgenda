package com.schoolagenda.dto.director;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassroomCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;
}