package com.cairo.cairobackend.entity;


//issue is the most important entity - it has the most
//relationships with other entities. Pay attention to fetch strategies here.
//We use LAZY everywhere to avoid loading the entire object
//graph whenever we fetch a list of issues.

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    //spring_id is nullable - null means the issue is in the backlog
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    //assignee is optional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private IssueType type = IssueType.TASK;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private IssueStatus status = IssueStatus.TODO;

    @Column(name = "story_points")
    @Builder.Default
    private Integer storyPoints = 0;

    //Comments cascade - deleting an issue deletes its comments
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    //History entries cascade
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IssueHistory> history = new ArrayList<>();

    //M:M with labels via the issue_labels join table
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "issue_labels",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    @Builder.Default
    private List<Label> labels = new ArrayList<>();

    @Column(name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum IssueType { BUG, STORY, TASK, EPIC }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum IssueStatus { TODO, IN_PROGRESS, IN_REVIEW, DONE}
}
