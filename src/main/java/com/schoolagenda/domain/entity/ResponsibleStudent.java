package com.schoolagenda.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "responsible_student",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"responsible_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_rs_responsible", columnList = "responsible_id"),
                @Index(name = "idx_rs_student", columnList = "student_id")
        }
)
@Getter @Setter @NoArgsConstructor
public class ResponsibleStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id", nullable = false)
    private Responsible responsible;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}