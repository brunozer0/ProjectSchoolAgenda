package com.schoolagenda.domain.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

import com.schoolagenda.domain.enums.NoteType;
import com.schoolagenda.domain.enums.NoteStatus;
@Entity
@Table(name = "notes", indexes = {
        @Index(name = "idx_notes_student", columnList = "student_id"),
        @Index(name = "idx_notes_author",  columnList = "author_id"),
        @Index(name = "idx_notes_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;


    @OneToMany(mappedBy = "note", fetch = FetchType.LAZY)
    private List<Image> images;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NoteType type;

    @Column(name = "is_visible_to_responsible", nullable = false)
    private boolean visibleToResponsible = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NoteStatus status = NoteStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}