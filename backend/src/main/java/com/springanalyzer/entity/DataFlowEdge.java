package com.springanalyzer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataFlowEdge {
    private String id;
    private String source; // Node ID
    private String target; // Node ID
    private String type; // CALLS, USES, RETURNS
    private String label;
    private String dataType; // Type of data being passed
    private boolean isTransformation; // True if data is transformed
    private String transformationDetails;
}
