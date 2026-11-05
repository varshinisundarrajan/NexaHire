package com.nexahire.service;

import com.nexahire.dto.KeywordMatchResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class ResumeParsingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParsingService.class);

    // Standard Industry Skill Taxonomy for automatic resume skill extraction
    private static final List<String> COMMON_SKILLS_DICTIONARY = List.of(
            "Java", "Spring Boot", "Spring", "Hibernate", "JPA", "Microservices", "REST API",
            "SQL", "MySQL", "PostgreSQL", "Oracle", "MongoDB", "Redis", "Cassandra",
            "JavaScript", "TypeScript", "React", "Angular", "Vue", "Next.js", "Node.js", "Express",
            "HTML5", "HTML", "CSS3", "CSS", "Tailwind CSS", "Bootstrap", "Sass",
            "Python", "Django", "Flask", "FastAPI", "C++", "C#", ".NET", "ASP.NET", "PHP", "Go", "Golang", "Rust",
            "AWS", "Amazon Web Services", "Azure", "GCP", "Google Cloud", "Docker", "Kubernetes", "Terraform",
            "Linux", "Unix", "Bash", "Shell", "CI/CD", "Jenkins", "GitHub Actions", "Git", "GitHub", "GitLab",
            "Agile", "Scrum", "JIRA", "Confluence", "Kafka", "RabbitMQ", "GraphQL", "Elasticsearch",
            "Figma", "UI/UX", "Data Analytics", "Machine Learning", "Product Management", "Wireframing"
    );

    /**
     * Extracts text content from a file (PDF, DOCX, TXT, or text-based documents).
     */
    public String extractTextFromFile(File file, String fileName) {
        if (file == null || !file.exists()) {
            return "";
        }

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return extractTextFromPdf(file, fileName);
        } else if (lowerName.endsWith(".docx")) {
            return extractTextFromDocx(file, fileName);
        } else {
            // Read as text or plain UTF-8
            return extractTextFromPlainText(file, fileName);
        }
    }

    private String extractTextFromPdf(File file, String fileName) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            log.warn("Failed to extract PDF text from {}: {}", fileName, e.getMessage());
            return "";
        }
    }

    private String extractTextFromDocx(File file, String fileName) {
        StringBuilder sb = new StringBuilder();
        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry entry = zipFile.getEntry("word/document.xml");
            if (entry != null) {
                try (InputStream is = zipFile.getInputStream(entry)) {
                    byte[] bytes = is.readAllBytes();
                    String xml = new String(bytes, StandardCharsets.UTF_8);
                    // Extract text inside <w:t> tags
                    Matcher matcher = Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(xml);
                    while (matcher.find()) {
                        String piece = matcher.group(1);
                        piece = piece.replace("&amp;", "&")
                                     .replace("&lt;", "<")
                                     .replace("&gt;", ">")
                                     .replace("&quot;", "\"")
                                     .replace("&apos;", "'");
                        sb.append(piece).append(" ");
                    }
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to extract DOCX text from {}: {}", fileName, e.getMessage());
            return "";
        }
    }

    private String extractTextFromPlainText(File file, String fileName) {
        try (InputStream is = new FileInputStream(file)) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to extract plain text from {}: {}", fileName, e.getMessage());
            return "";
        }
    }

    /**
     * Extracts known technical and domain skills from raw resume text.
     */
    public List<String> extractSkillsFromText(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String lowerText = text.toLowerCase();
        List<String> foundSkills = new ArrayList<>();

        for (String skill : COMMON_SKILLS_DICTIONARY) {
            if (isSkillPresent(skill.toLowerCase(), lowerText)) {
                foundSkills.add(skill);
            }
        }

        return foundSkills;
    }

    /**
     * Calculates transparent keyword match score between job requirements and candidate profile/resume text.
     */
    public KeywordMatchResult calculateMatch(String requiredSkillsStr, String candidateSkillsStr, String resumeText, String coverLetter) {
        if (requiredSkillsStr == null || requiredSkillsStr.isBlank()) {
            return new KeywordMatchResult(100, Collections.emptyList(), Collections.emptyList(), 0, 0);
        }

        List<String> requiredSkills = parseSkillsList(requiredSkillsStr);
        if (requiredSkills.isEmpty()) {
            return new KeywordMatchResult(100, Collections.emptyList(), Collections.emptyList(), 0, 0);
        }

        String candidateCorpus = ((candidateSkillsStr != null ? candidateSkillsStr : "") + " " +
                                  (resumeText != null ? resumeText : "") + " " +
                                  (coverLetter != null ? coverLetter : "")).toLowerCase();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : requiredSkills) {
            String cleanSkill = skill.trim();
            if (cleanSkill.isEmpty()) continue;

            if (isSkillPresent(cleanSkill.toLowerCase(), candidateCorpus)) {
                matched.add(cleanSkill);
            } else {
                missing.add(cleanSkill);
            }
        }

        int totalRequired = matched.size() + missing.size();
        int score = totalRequired > 0 ? (int) Math.round(((double) matched.size() / totalRequired) * 100.0) : 0;

        return new KeywordMatchResult(score, matched, missing, totalRequired, matched.size());
    }

    public KeywordMatchResult calculateMatch(String requiredSkillsStr, String candidateSkillsStr, String resumeText) {
        return calculateMatch(requiredSkillsStr, candidateSkillsStr, resumeText, "");
    }

    private boolean isSkillPresent(String skill, String text) {
        if (skill.length() <= 2) {
            // For short acronyms like "c", "r", "go", "js", use word boundary regex
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE);
            return pattern.matcher(text).find();
        }

        // Check word boundaries for standard skill words
        Pattern pattern = Pattern.compile("(?:^|[^a-zA-Z0-9])" + Pattern.quote(skill) + "(?:$|[^a-zA-Z0-9])", Pattern.CASE_INSENSITIVE);
        if (pattern.matcher(text).find()) {
            return true;
        }

        // Substring check for compound terms like "REST API", "Spring Boot", etc.
        return text.contains(skill);
    }

    public List<String> parseSkillsList(String skillsString) {
        if (skillsString == null || skillsString.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(skillsString.split("[,;|\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
