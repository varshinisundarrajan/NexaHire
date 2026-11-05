package com.nexahire.controller;

import com.nexahire.dto.ApiResponse;
import com.nexahire.dto.JobRequest;
import com.nexahire.entity.Job;
import com.nexahire.service.JobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Job>>> searchJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Page<Job> jobs = jobService.searchJobs(search, department, jobType, status, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Job>> createJob(@Valid @RequestBody JobRequest request) {
        Job created = jobService.createJob(request);
        return ResponseEntity.ok(ApiResponse.ok("Job opening created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Job>> updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        Job updated = jobService.updateJob(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Job opening updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.ok("Job opening deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Job>> updateJobStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Job updated = jobService.updateJobStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Job status updated to " + status, updated));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<List<Job>>> getMyJobs() {
        List<Job> myJobs = jobService.getMyJobs();
        return ResponseEntity.ok(ApiResponse.ok(myJobs));
    }
}
