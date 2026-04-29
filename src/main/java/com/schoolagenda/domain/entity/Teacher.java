package com.schoolagenda.domain.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teachers")
@Getter @Setter @NoArgsConstructor
public class Teacher {

    @Id
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @JsonIgnore
    @ManyToMany(mappedBy = "teachers", fetch = FetchType.LAZY)
    private List<Classroom> classrooms = new ArrayList<>();

    public boolean hasClassrooms() {
        return !classrooms.isEmpty();
    }
}