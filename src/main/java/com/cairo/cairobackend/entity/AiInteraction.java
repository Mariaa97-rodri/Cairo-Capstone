package com.cairo.cairobackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_interactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AiFeature feature;

    @NotBlank
    @Column(name = "prompt_text", nullable = false,
            columnDefinition = "TEXT")
    private String promptText;

    @NotBlank
    @Column(name = "response_text", nullable = false,
            columnDefinition = "TEXT")
    private String responseText;

    // This is the field the builder needs for .tokensUsed()
    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private Integer tokensUsed = 0;

    @Column(name = "was_accepted", nullable = false)
    @Builder.Default
    private Boolean wasAccepted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum AiFeature {
        DESCRIBE_ISSUE,
        SUGGEST_FIELDS,
        RECOMMEND_ASSIGNEE,
        SPRINT_SUMMARY
    }
}