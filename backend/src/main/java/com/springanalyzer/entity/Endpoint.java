package com.springanalyzer.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "endpoints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endpoint {

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
    private HttpMethod httpMethod;

    @Column(nullable = false)
    private String path;

    @Column(name = "method_name")
    private String methodName;

    @Column(name = "return_type")
    private String returnType;

    @Column(columnDefinition = "TEXT")
    private String parameters;
}
