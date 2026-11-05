package com.nexahire.controller;

import com.nexahire.dto.ApiResponse;
import com.nexahire.dto.InterviewFeedbackRequest;
import com.nexahire.dto.InterviewRequest;
import com.nexahire.entity.Interview;
import com.nexahire.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Interview>> scheduleInterview(@Valid @RequestBody InterviewRequest request) {
        Interview scheduled = interviewService.scheduleInterview(request);
        return ResponseEntity.ok(ApiResponse.ok("Interview scheduled successfully", scheduled));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<List<Interview>>> getUpcomingInterviews() {
        List<Interview> upcoming = interviewService.getUpcomingInterviews();
        return ResponseEntity.ok(ApiResponse.ok(upcoming));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Interview>>> getMyInterviews() {
        List<Interview> myInterviews = interviewService.getCandidateInterviews();
        return ResponseEntity.ok(ApiResponse.ok(myInterviews));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<List<Interview>>> getAllInterviews() {
        List<Interview> all = interviewService.getAllInterviews();
        return ResponseEntity.ok(ApiResponse.ok(all));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Interview>> getInterviewById(@PathVariable Long id) {
        Interview interview = interviewService.getInterviewById(id);
        return ResponseEntity.ok(ApiResponse.ok(interview));
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Interview>> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody InterviewFeedbackRequest request
    ) {
        Interview updated = interviewService.submitFeedback(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Interview feedback submitted successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR_MANAGER', 'ROLE_RECRUITER')")
    public ResponseEntity<ApiResponse<Interview>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Interview updated = interviewService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Interview status updated to " + status, updated));
    }
}
