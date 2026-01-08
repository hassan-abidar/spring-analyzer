package com.springanalyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataFlowNode {
    private String id;
    private String name;
    private String type; // API, SERVICE, REPOSITORY, ENTITY, DTO
    private String className;
    private String methodName;
    private int layer; // 0: API, 1: Service, 2: Repository, 3: Entity
    private String description;
    private List<String> dtoNames;
    private List<String> entityNames;
    private String methodSignature;
}
