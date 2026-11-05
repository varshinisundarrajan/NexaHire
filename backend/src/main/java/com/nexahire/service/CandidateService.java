package com.nexahire.service;
import java.util.Set;
import java.util.LinkedHashSet;

import com.nexahire.dto.CandidateProfileRequest;
import com.nexahire.entity.CandidateProfile;
import com.nexahire.entity.Resume;
import com.nexahire.entity.User;
import com.nexahire.exception.BadRequestException;
import com.nexahire.exception.ResourceNotFoundException;
import com.nexahire.repository.CandidateProfileRepository;
import com.nexahire.repository.ResumeRepository;
import com.nexahire.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
public class CandidateService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final ResumeParsingService resumeParsingService;
    private final AuditService auditService;

    public CandidateService(CandidateProfileRepository candidateProfileRepository,
                            ResumeRepository resumeRepository,
                            UserRepository userRepository,
                            AuthService authService,
                            FileStorageService fileStorageService,
                            ResumeParsingService resumeParsingService,
                            AuditService auditService) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.resumeParsingService = resumeParsingService;
        this.auditService = auditService;
    }

    public Page<CandidateProfile> searchCandidates(String keyword, int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        String cleanKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return candidateProfileRepository.searchCandidates(cleanKeyword, pageable);
    }

    public CandidateProfile getCandidateById(Long id) {
        return candidateProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found with id: " + id));
    }

    public CandidateProfile getCurrentCandidateProfile() {
        User currentUser = authService.getCurrentUser();
        return candidateProfileRepository.findByUser(currentUser)
                .orElseGet(() -> {
                    CandidateProfile profile = new CandidateProfile(currentUser);
                    return candidateProfileRepository.save(profile);
                });
    }

    @Transactional
    public CandidateProfile updateProfile(CandidateProfileRequest request) {
        CandidateProfile profile = getCurrentCandidateProfile();
        User user = profile.getUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        userRepository.save(user);

        profile.setHeadline(request.getHeadline());
        profile.setSummary(request.getSummary());
        profile.setSkills(request.getSkills());
        if (request.getExperienceYears() != null) {
            profile.setExperienceYears(request.getExperienceYears());
        }
        profile.setEducation(request.getEducation());
        profile.setLocation(request.getLocation());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setCurrentSalary(request.getCurrentSalary());
        profile.setExpectedSalary(request.getExpectedSalary());

        CandidateProfile updated = candidateProfileRepository.save(profile);
        auditService.log("UPDATE_PROFILE", "CANDIDATE", updated.getId(), "Candidate updated profile: " + user.getFullName());
        return updated;
    }

    @Transactional
    public Resume uploadResume(MultipartFile file) {
        CandidateProfile profile = getCurrentCandidateProfile();

        String storedFileName = fileStorageService.storeFile(file);
        File savedFile = fileStorageService.getFile(storedFileName);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String extractedText = resumeParsingService.extractTextFromFile(savedFile, originalFilename);
        List<String> extractedSkills = resumeParsingService.extractSkillsFromText(extractedText);

        Resume resume = new Resume(
                profile,
                originalFilename,
                file.getContentType() != null ? file.getContentType() : "application/pdf",
                file.getSize(),
                storedFileName
        );
        resume.setParsedText(extractedText);
        resume.setParsedSkills(String.join(", ", extractedSkills));

        Resume savedResume = resumeRepository.save(resume);

        // If candidate profile skills are empty or new skills found, merge them into candidate profile
        if (!extractedSkills.isEmpty()) {
            Set<String> existingSkills = new LinkedHashSet<>(resumeParsingService.parseSkillsList(profile.getSkills()));
            existingSkills.addAll(extractedSkills);
            profile.setSkills(String.join(", ", existingSkills));
            candidateProfileRepository.save(profile);
        }

        auditService.log("UPLOAD_RESUME", "RESUME", savedResume.getId(), "Uploaded and parsed resume: " + originalFilename + " (" + extractedSkills.size() + " skills detected)");
        return savedResume;
    }

    public File getResumeFile(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Invalid file path");
        }
        File file = fileStorageService.getFile(filename);
        if (!file.exists()) {
            throw new ResourceNotFoundException("Resume file not found: " + filename);
        }
        return file;
    }

    public List<Resume> getCandidateResumes(Long candidateId) {
        return resumeRepository.findByCandidateIdOrderByUploadedAtDesc(candidateId);
    }

    public Resume getLatestResume(Long candidateId) {
        return resumeRepository.findFirstByCandidateIdOrderByUploadedAtDesc(candidateId)
                .orElse(null);
    }
}
