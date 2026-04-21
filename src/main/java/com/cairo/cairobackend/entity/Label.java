package com.cairo.cairobackend.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "labels", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id","name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank
    @Size(max =50)
    @Column(nullable = false, length = 50)
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be valid hex code like #6366F1")
    @Column(nullable = false, length = 7)
    @Builder.Default
    private String color = "#6366F1";
}

