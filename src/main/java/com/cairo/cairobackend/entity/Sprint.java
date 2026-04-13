package com.cairo.cairobackend.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sprints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id",nullable = false)
    private Project project;

    @NotBlank(message = "Sprint name is required")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SprintStatus status = SprintStatus.PENDING;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    //Issues in this sprint - when a sprint is deleted,
    //ON DELETE SET NULL means issues aren't deleted,
    //just their sprint_id is set to null (backlog).
    //We model that here with CascadeType.MERGE only -
    // no orphanRemoval so issues survive sprint deletion.
    @OneToMany(mappedBy = "sprint")
    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum SprintStatus { PENDING, ACTIVE, COMPLETED }
}
