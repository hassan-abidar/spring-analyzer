package com.springanalyzer.service;

import com.springanalyzer.entity.*;
import com.springanalyzer.repository.SecurityIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityScannerService {

    private final SecurityIssueRepository issueRepository;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(password|passwd|pwd|secret|api[_-]?key|apikey|token|auth)\\s*=\\s*[\"'][^\"']+[\"']",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "\"\\s*\\+\\s*\\w+|\\w+\\s*\\+\\s*\".*(?:SELECT|INSERT|UPDATE|DELETE|FROM|WHERE)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXEC_PATTERN = Pattern.compile(
        "Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder"
    );

    private static final Pattern WEAK_CRYPTO_PATTERN = Pattern.compile(
        "MD5|SHA-?1|DES|RC4",
        Pattern.CASE_INSENSITIVE
    );

    public List<SecurityIssue> scanProject(Project project, Path extractedPath, Map<String, AnalyzedClass> classMap) {
        List<SecurityIssue> issues = new ArrayList<>();

        try {
            Files.walk(extractedPath)
                .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".properties") || p.toString().endsWith(".yml"))
                .forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        String fileName = extractedPath.relativize(file).toString();
                        
                        issues.addAll(scanForHardcodedSecrets(project, content, fileName));
                        
                        if (fileName.endsWith(".java")) {
                            issues.addAll(scanForSqlInjection(project, content, fileName, classMap));
                            issues.addAll(scanForCommandInjection(project, content, fileName));
                            issues.addAll(scanForWeakCrypto(project, content, fileName));
                            issues.addAll(scanForMissingAuth(project, content, fileName, classMap));
                            issues.addAll(scanForInsecureEndpoints(project, content, fileName));
                        }
                        
                        if (fileName.endsWith(".properties") || fileName.endsWith(".yml")) {
                            issues.addAll(scanConfigFile(project, content, fileName));
                        }
                    } catch (IOException e) {
                        log.warn("Failed to scan file: {}", file, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to walk project directory", e);
        }

        issueRepository.saveAll(issues);
        return issues;
    }

    private List<SecurityIssue> scanForHardcodedSecrets(Project project, String content, String fileName) {
        List<SecurityIssue> issues = new ArrayList<>();
        Matcher matcher = PASSWORD_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String match = matcher.group();
            if (!isLikelyPlaceholder(match)) {
                int lineNum = getLineNumber(content, matcher.start());
                issues.add(SecurityIssue.builder()
                    .project(project)
                    .severity(IssueSeverity.HIGH)
                    .category(IssueCategory.HARDCODED_SECRET)
                    .title("Potential hardcoded secret")
                    .description("Found potential hardcoded credential or secret key")
                    .fileName(fileName)
                    .lineNumber(lineNum)
                    .codeSnippet(truncate(match, 100))
                    .recommendation("Use environment variables or a secrets manager")
                    .build());
            }
        }
        return issues;
    }

    private List<SecurityIssue> scanForSqlInjection(Project project, String content, String fileName, Map<String, AnalyzedClass> classMap) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        if (content.contains("createQuery") || content.contains("createNativeQuery") || content.contains("executeQuery")) {
            Matcher matcher = SQL_INJECTION_PATTERN.matcher(content);
            while (matcher.find()) {
                int lineNum = getLineNumber(content, matcher.start());
                issues.add(SecurityIssue.builder()
                    .project(project)
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.SQL_INJECTION)
                    .title("Potential SQL injection")
                    .description("String concatenation in SQL query detected")
                    .fileName(fileName)
                    .lineNumber(lineNum)
                    .codeSnippet(truncate(matcher.group(), 100))
                    .recommendation("Use parameterized queries or JPA named parameters")
                    .build());
            }
        }
        return issues;
    }

    private List<SecurityIssue> scanForCommandInjection(Project project, String content, String fileName) {
        List<SecurityIssue> issues = new ArrayList<>();
        Matcher matcher = EXEC_PATTERN.matcher(content);
        
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            issues.add(SecurityIssue.builder()
                .project(project)
                .severity(IssueSeverity.HIGH)
                .category(IssueCategory.OTHER)
                .title("Command execution detected")
                .description("Direct command execution can lead to command injection if user input is involved")
                .fileName(fileName)
                .lineNumber(lineNum)
                .recommendation("Validate and sanitize all input, avoid shell commands if possible")
                .build());
        }
        return issues;
    }

    private List<SecurityIssue> scanForWeakCrypto(Project project, String content, String fileName) {
        List<SecurityIssue> issues = new ArrayList<>();
        Matcher matcher = WEAK_CRYPTO_PATTERN.matcher(content);
        
        while (matcher.find()) {
            int lineNum = getLineNumber(content, matcher.start());
            issues.add(SecurityIssue.builder()
                .project(project)
                .severity(IssueSeverity.MEDIUM)
                .category(IssueCategory.WEAK_CRYPTO)
                .title("Weak cryptographic algorithm: " + matcher.group())
                .description("Usage of deprecated or weak cryptographic algorithm")
                .fileName(fileName)
                .lineNumber(lineNum)
                .recommendation("Use SHA-256 or stronger algorithms, AES for encryption")
                .build());
        }
        return issues;
    }

    private List<SecurityIssue> scanForMissingAuth(Project project, String content, String fileName, Map<String, AnalyzedClass> classMap) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        boolean isController = content.contains("@RestController") || content.contains("@Controller");
        boolean hasSecurityAnnotation = content.contains("@PreAuthorize") || 
                                        content.contains("@Secured") || 
                                        content.contains("@RolesAllowed");
        
        if (isController && !hasSecurityAnnotation) {
            boolean hasSensitiveEndpoints = content.contains("@DeleteMapping") || 
                                           content.contains("@PutMapping") ||
                                           (content.contains("@PostMapping") && !content.contains("/login") && !content.contains("/register"));
            
            if (hasSensitiveEndpoints) {
                issues.add(SecurityIssue.builder()
                    .project(project)
                    .severity(IssueSeverity.MEDIUM)
                    .category(IssueCategory.MISSING_AUTH)
                    .title("Controller without security annotations")
                    .description("Controller has modifying endpoints without explicit security annotations")
                    .fileName(fileName)
                    .recommendation("Add @PreAuthorize, @Secured, or @RolesAllowed annotations")
                    .build());
            }
        }
        return issues;
    }

    private List<SecurityIssue> scanForInsecureEndpoints(Project project, String content, String fileName) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        if (content.contains("@CrossOrigin") && content.contains("*")) {
            issues.add(SecurityIssue.builder()
                .project(project)
                .severity(IssueSeverity.MEDIUM)
                .category(IssueCategory.CORS_MISCONFIGURATION)
                .title("Overly permissive CORS configuration")
                .description("CORS is configured to allow all origins")
                .fileName(fileName)
                .recommendation("Restrict CORS to specific trusted origins")
                .build());
        }
        return issues;
    }

    private List<SecurityIssue> scanConfigFile(Project project, String content, String fileName) {
        List<SecurityIssue> issues = new ArrayList<>();
        
        if (content.contains("debug=true") || content.contains("debug: true")) {
            issues.add(SecurityIssue.builder()
                .project(project)
                .severity(IssueSeverity.LOW)
                .category(IssueCategory.DEBUG_ENABLED)
                .title("Debug mode enabled")
                .description("Debug mode should be disabled in production")
                .fileName(fileName)
                .recommendation("Set debug=false for production deployments")
                .build());
        }
        
        if (content.contains("spring.h2.console.enabled=true")) {
            issues.add(SecurityIssue.builder()
                .project(project)
                .severity(IssueSeverity.HIGH)
                .category(IssueCategory.SENSITIVE_DATA_EXPOSURE)
                .title("H2 Console enabled")
                .description("H2 database console is enabled, exposing database access")
                .fileName(fileName)
                .recommendation("Disable H2 console in production")
                .build());
        }
        
        return issues;
    }

    private boolean isLikelyPlaceholder(String match) {
        return match.contains("${") || match.contains("@Value") || 
               match.contains("example") || match.contains("placeholder") ||
               match.contains("changeme") || match.contains("xxx");
    }

    private int getLineNumber(String content, int position) {
        return (int) content.substring(0, position).chars().filter(c -> c == '\n').count() + 1;
    }

    private String truncate(String str, int maxLen) {
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}
