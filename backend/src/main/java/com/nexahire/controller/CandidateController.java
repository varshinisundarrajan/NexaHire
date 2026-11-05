package com.nexahire.controller;

import com.nexahire.dto.ApiResponse;
import com.nexahire.dto.CandidateProfileRequest;
import com.nexahire.entity.CandidateProfile;
import com.nexahire.entity.Resume;
import com.nexahire.service.CandidateService;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Page<CandidateProfile>>> searchCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<CandidateProfile> candidates = candidateService.searchCandidates(search, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.ok(candidates));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<CandidateProfile>> getCandidateById(@PathVariable Long id) {
        CandidateProfile candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(ApiResponse.ok(candidate));
    }

    @GetMapping("/profile/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> getMyProfile() {
        CandidateProfile profile = candidateService.getCurrentCandidateProfile();
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> updateMyProfile(@RequestBody CandidateProfileRequest request) {
        CandidateProfile updated = candidateService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Resume>> uploadResume(@RequestParam("file") MultipartFile file) {
        Resume resume = candidateService.uploadResume(file);
        return ResponseEntity.ok(ApiResponse.ok("Resume uploaded and parsed successfully", resume));
    }

    @GetMapping("/{id}/resumes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER', 'ROLE_CANDIDATE')")
    public ResponseEntity<ApiResponse<List<Resume>>> getCandidateResumes(@PathVariable Long id) {
        List<Resume> resumes = candidateService.getCandidateResumes(id);
        return ResponseEntity.ok(ApiResponse.ok(resumes));
    }

    @GetMapping("/resume/download/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadResume(@PathVariable String filename) {
        java.io.File file = candidateService.getResumeFile(filename);
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);

        String contentType = "application/octet-stream";
        if (filename.toLowerCase().endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (filename.toLowerCase().endsWith(".docx")) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
