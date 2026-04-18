package com.schoolagenda.dto.director;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FamilyCreateResponse {
    private Long responsibleId;
    private Long userId;
    private String name;
    private String email;
    private List<Long> studentIds;
}
