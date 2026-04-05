package com.schoolagenda.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "responsibles")
@Getter @Setter @NoArgsConstructor
public class Responsible {

    @Id
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @JsonIgnore
    @OneToMany(mappedBy = "responsible", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ResponsibleStudent> responsibleStudents = new ArrayList<>();

    public boolean hasStudents() {
        return !responsibleStudents.isEmpty();
    }
}