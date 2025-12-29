package com.springanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "security_issues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private AnalyzedClass analyzedClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueCategory category;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String fileName;
    private Integer lineNumber;

    @Column(length = 500)
    private String codeSnippet;

    @Column(length = 500)
    private String recommendation;
}
