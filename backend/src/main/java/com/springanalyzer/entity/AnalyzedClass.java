package com.springanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "analyzed_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyzedClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "full_path")
    private String fullPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassType type;

    @Column(columnDefinition = "TEXT")
    private String annotations;

    @Column(name = "extends_class")
    private String extendsClass;

    @Column(columnDefinition = "TEXT")
    private String implementsInterfaces;

    @Column(name = "field_count")
    private Integer fieldCount;

    @Column(name = "method_count")
    private Integer methodCount;

    @Column(name = "module_name")
    private String moduleName;
}
