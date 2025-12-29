package com.springanalyzer.service;

import com.springanalyzer.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class JavaParserService {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(public\\s+)?(abstract\\s+)?(class|interface|enum)\\s+(\\w+)");
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("extends\\s+(\\w+)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("implements\\s+([\\w,\\s]+)");
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@(\\w+)(?:\\([^)]*\\))?");
    private static final Pattern FIELD_PATTERN = Pattern.compile("(private|protected|public)\\s+(?!class|interface|enum)\\w+(?:<[^>]+>)?\\s+\\w+\\s*[;=]");
    private static final Pattern METHOD_PATTERN = Pattern.compile("(public|private|protected)\\s+(?!class)\\w+(?:<[^>]+>)?\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?");
    
    private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile("@(Get|Post|Put|Delete|Patch|Request)Mapping\\s*(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"|\\(\\s*(?:value\\s*=\\s*)?'([^']*)'|\\(\\s*\"([^\"]*)\"|\\))?");
    private static final Pattern SIMPLE_MAPPING_PATTERN = Pattern.compile("@(Get|Post|Put|Delete|Patch)Mapping(?:\\s*\\(\\s*[\"']([^\"']*)[\"']\\s*\\))?");
    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("(public|private|protected)\\s+(\\w+(?:<[^>]+>)?)\\s+(\\w+)\\s*\\(([^)]*)\\)");

    public ParsedClass parseJavaFile(Path file) {
        try {
            String content = Files.readString(file);
            return parseContent(content, file.toString());
        } catch (IOException e) {
            log.error("Failed to parse file: {}", file, e);
            return null;
        }
    }

    private ParsedClass parseContent(String content, String filePath) {
        ParsedClass parsed = new ParsedClass();
        parsed.setFullPath(filePath);

        Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
        if (packageMatcher.find()) {
            parsed.setPackageName(packageMatcher.group(1));
        }

        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        if (classMatcher.find()) {
            parsed.setName(classMatcher.group(4));
            String classType = classMatcher.group(3);
            if ("interface".equals(classType)) {
                parsed.setClassType(ClassType.INTERFACE);
            } else if ("enum".equals(classType)) {
                parsed.setClassType(ClassType.ENUM);
            }
        }

        List<String> annotations = new ArrayList<>();
        Matcher annotationMatcher = ANNOTATION_PATTERN.matcher(content);
        while (annotationMatcher.find()) {
            annotations.add(annotationMatcher.group(1));
        }
        parsed.setAnnotations(annotations);

        if (parsed.getClassType() == null) {
            parsed.setClassType(determineClassType(annotations));
        }

        Matcher extendsMatcher = EXTENDS_PATTERN.matcher(content);
        if (extendsMatcher.find()) {
            parsed.setExtendsClass(extendsMatcher.group(1));
        }

        Matcher implementsMatcher = IMPLEMENTS_PATTERN.matcher(content);
        if (implementsMatcher.find()) {
            String[] interfaces = implementsMatcher.group(1).split(",");
            parsed.setImplementsInterfaces(Arrays.stream(interfaces)
                    .map(String::trim)
                    .toList());
        }

        Matcher fieldMatcher = FIELD_PATTERN.matcher(content);
        int fieldCount = 0;
        while (fieldMatcher.find()) fieldCount++;
        parsed.setFieldCount(fieldCount);

        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        int methodCount = 0;
        while (methodMatcher.find()) methodCount++;
        parsed.setMethodCount(methodCount);

        if (parsed.getClassType() == ClassType.REST_CONTROLLER || parsed.getClassType() == ClassType.CONTROLLER) {
            parsed.setEndpoints(parseEndpoints(content));
        }

        return parsed;
    }

    private ClassType determineClassType(List<String> annotations) {
        for (String annotation : annotations) {
            switch (annotation) {
                case "RestController": return ClassType.REST_CONTROLLER;
                case "Controller": return ClassType.CONTROLLER;
                case "Service": return ClassType.SERVICE;
                case "Repository": return ClassType.REPOSITORY;
                case "Entity": return ClassType.ENTITY;
                case "Component": return ClassType.COMPONENT;
                case "Configuration": return ClassType.CONFIGURATION;
            }
        }
        return ClassType.OTHER;
    }

    private List<ParsedEndpoint> parseEndpoints(String content) {
        List<ParsedEndpoint> endpoints = new ArrayList<>();
        
        String classPath = "";
        Pattern classMapping = Pattern.compile("@RequestMapping\\s*\\([^)]*[\"']([^\"']+)[\"'][^)]*\\)");
        Matcher classPathMatcher = classMapping.matcher(content);
        if (classPathMatcher.find()) {
            classPath = classPathMatcher.group(1);
        }

        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher mappingMatcher = SIMPLE_MAPPING_PATTERN.matcher(line);
            
            if (mappingMatcher.find()) {
                String mappingType = mappingMatcher.group(1);
                String path = mappingMatcher.group(2);
                if (path == null || path.isEmpty()) path = "";
                
                if (path.length() > 200) continue;
                
                HttpMethod httpMethod = switch (mappingType) {
                    case "Get" -> HttpMethod.GET;
                    case "Post" -> HttpMethod.POST;
                    case "Put" -> HttpMethod.PUT;
                    case "Delete" -> HttpMethod.DELETE;
                    case "Patch" -> HttpMethod.PATCH;
                    default -> HttpMethod.GET;
                };

                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    Matcher methodSig = METHOD_SIGNATURE_PATTERN.matcher(lines[j]);
                    if (methodSig.find()) {
                        ParsedEndpoint endpoint = new ParsedEndpoint();
                        endpoint.setHttpMethod(httpMethod);
                        endpoint.setPath(normalizePath(classPath, path));
                        endpoint.setMethodName(methodSig.group(3));
                        endpoint.setReturnType(methodSig.group(2));
                        endpoint.setParameters(methodSig.group(4));
                        endpoints.add(endpoint);
                        break;
                    }
                }
            }
        }
        
        return endpoints;
    }

    private String normalizePath(String classPath, String methodPath) {
        String fullPath = classPath + "/" + methodPath;
        return "/" + fullPath.replaceAll("/+", "/").replaceAll("^/|/$", "");
    }

    public static class ParsedClass {
        private String name;
        private String packageName;
        private String fullPath;
        private ClassType classType;
        private List<String> annotations = new ArrayList<>();
        private String extendsClass;
        private List<String> implementsInterfaces = new ArrayList<>();
        private int fieldCount;
        private int methodCount;
        private List<ParsedEndpoint> endpoints = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        public String getFullPath() { return fullPath; }
        public void setFullPath(String fullPath) { this.fullPath = fullPath; }
        public ClassType getClassType() { return classType; }
        public void setClassType(ClassType classType) { this.classType = classType; }
        public List<String> getAnnotations() { return annotations; }
        public void setAnnotations(List<String> annotations) { this.annotations = annotations; }
        public String getExtendsClass() { return extendsClass; }
        public void setExtendsClass(String extendsClass) { this.extendsClass = extendsClass; }
        public List<String> getImplementsInterfaces() { return implementsInterfaces; }
        public void setImplementsInterfaces(List<String> implementsInterfaces) { this.implementsInterfaces = implementsInterfaces; }
        public int getFieldCount() { return fieldCount; }
        public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
        public int getMethodCount() { return methodCount; }
        public void setMethodCount(int methodCount) { this.methodCount = methodCount; }
        public List<ParsedEndpoint> getEndpoints() { return endpoints; }
        public void setEndpoints(List<ParsedEndpoint> endpoints) { this.endpoints = endpoints; }
    }

    public static class ParsedEndpoint {
        private HttpMethod httpMethod;
        private String path;
        private String methodName;
        private String returnType;
        private String parameters;

        public HttpMethod getHttpMethod() { return httpMethod; }
        public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }
        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
    }
}
