package com.cairo.cairobackend.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Project key is required")
    @Size(max = 10, message = "Project key cannot exceed 10 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Project key must be uppercase letters and numbers onyl")
    @Column(name = "project-key", nullable = false, unique = true, length = 10)
    private String projectKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    //Many project can be owned by one user.
    //LAZY = don't load the owner object until we actually access it.
    //This prevents N+1 queries when listing projects.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    //One project has many sprints.
    //CascadeType.ALL + orphanRemoval = if we delete the project,
    //all its sprints are deleted too (matches ON DELETE CASCADE in SQL).
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Sprint> sprints = new ArrayList<>();

    //One project has many issues
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Issue> issues = new ArrayList<>();

    //One project has many labels
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Label> labels = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
