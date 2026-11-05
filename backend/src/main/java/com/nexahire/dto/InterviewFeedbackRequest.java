package com.nexahire.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class InterviewFeedbackRequest {

    @NotBlank(message = "Feedback comments are required")
    private String feedback;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    private String recommendation = "HIRE"; // HIRE, REJECT, HOLD
    private String status = "COMPLETED"; // COMPLETED, RESCHEDULED, CANCELLED

    public InterviewFeedbackRequest() {}

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
