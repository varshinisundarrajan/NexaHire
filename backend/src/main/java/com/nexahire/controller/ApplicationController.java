package com.nexahire.controller;

import com.nexahire.dto.ApiResponse;
import com.nexahire.dto.ApplicationRequest;
import com.nexahire.dto.ApplicationStatusUpdate;
import com.nexahire.entity.Application;
import com.nexahire.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('ROLE_CANDIDATE')")
    public ResponseEntity<ApiResponse<Application>> apply(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.apply(request);
        return ResponseEntity.ok(ApiResponse.ok("Application submitted successfully with " + application.getMatchScore() + "% keyword match!", application));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Page<Application>>> searchApplications(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "appliedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<Application> applications = applicationService.searchApplications(jobId, status, search, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.ok(applications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Application>> getApplicationById(@PathVariable Long id) {
        Application application = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApiResponse.ok(application));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_CANDIDATE')")
    public ResponseEntity<ApiResponse<List<Application>>> getMyApplications() {
        List<Application> myApps = applicationService.getMyApplications();
        return ResponseEntity.ok(ApiResponse.ok(myApps));
    }

    @GetMapping("/pipeline/{jobId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<List<Application>>> getPipelineApplications(@PathVariable Long jobId) {
        List<Application> pipelineApps = applicationService.getApplicationsByJob(jobId);
        return ResponseEntity.ok(ApiResponse.ok(pipelineApps));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Application>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdate update
    ) {
        Application updated = applicationService.updateStatus(id, update);
        return ResponseEntity.ok(ApiResponse.ok("Application stage updated to " + updated.getStatus(), updated));
    }
}
