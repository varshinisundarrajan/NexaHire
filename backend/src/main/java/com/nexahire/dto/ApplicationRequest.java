package com.nexahire.dto;

import jakarta.validation.constraints.NotNull;

public class ApplicationRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private Long resumeId;
    private String coverLetter;

    public ApplicationRequest() {}

    public ApplicationRequest(Long jobId, Long resumeId, String coverLetter) {
        this.jobId = jobId;
        this.resumeId = resumeId;
        this.coverLetter = coverLetter;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
}
