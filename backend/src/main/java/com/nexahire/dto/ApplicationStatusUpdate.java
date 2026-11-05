package com.nexahire.dto;

import jakarta.validation.constraints.NotBlank;

public class ApplicationStatusUpdate {

    @NotBlank(message = "Status is required")
    private String status; // APPLIED, SCREENING, SHORTLISTED, INTERVIEW, SELECTED, REJECTED

    private String recruiterNotes;

    public ApplicationStatusUpdate() {}

    public ApplicationStatusUpdate(String status, String recruiterNotes) {
        this.status = status;
        this.recruiterNotes = recruiterNotes;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecruiterNotes() { return recruiterNotes; }
    public void setRecruiterNotes(String recruiterNotes) { this.recruiterNotes = recruiterNotes; }
}
