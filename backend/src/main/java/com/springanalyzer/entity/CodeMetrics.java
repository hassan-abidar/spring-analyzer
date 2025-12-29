package com.springanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_metrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    private int totalFiles;
    private int totalLines;
    private int codeLines;
    private int commentLines;
    private int blankLines;

    private int totalPackages;
    private int maxPackageDepth;

    private double avgMethodsPerClass;
    private double avgFieldsPerClass;
    private int maxMethodsInClass;
    private int maxFieldsInClass;

    private int publicClasses;
    private int publicMethods;
    private int privateFields;

    @Column(length = 2000)
    private String packageStructure;
}
