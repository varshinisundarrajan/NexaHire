package com.nexahire.service;

import com.nexahire.dto.InterviewFeedbackRequest;
import com.nexahire.dto.InterviewRequest;
import com.nexahire.entity.Application;
import com.nexahire.entity.Interview;
import com.nexahire.entity.User;
import com.nexahire.exception.ResourceNotFoundException;
import com.nexahire.repository.ApplicationRepository;
import com.nexahire.repository.InterviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public InterviewService(InterviewRepository interviewRepository,
                            ApplicationRepository applicationRepository,
                            AuthService authService,
                            NotificationService notificationService,
                            AuditService auditService) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Interview scheduleInterview(InterviewRequest request) {
        User interviewer = authService.getCurrentUser();
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + request.getApplicationId()));

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setInterviewer(interviewer);
        interview.setInterviewDate(request.getInterviewDate());
        interview.setInterviewTime(request.getInterviewTime());
        interview.setInterviewType(request.getInterviewType() != null ? request.getInterviewType() : "VIDEO");
        interview.setMeetingLink(request.getMeetingLink() != null ? request.getMeetingLink() : "https://meet.google.com/nexahire-interview");
        interview.setStatus("SCHEDULED");

        Interview saved = interviewRepository.save(interview);

        // Update application status to INTERVIEW
        application.setStatus("INTERVIEW");
        applicationRepository.save(application);

        // Send notification & email to candidate
        notificationService.sendNotification(
                application.getCandidate().getUser(),
                "Interview Scheduled: " + application.getJob().getTitle(),
                "Your interview for position '" + application.getJob().getTitle() + "' is scheduled on " +
                interview.getInterviewDate() + " at " + interview.getInterviewTime() + " (" + interview.getInterviewType() + "). Meeting Link: " + interview.getMeetingLink(),
                "INTERVIEW_INVITE",
                "/interviews.html"
        );

        auditService.log("SCHEDULE_INTERVIEW", "INTERVIEW", saved.getId(),
                "Scheduled interview for " + application.getCandidate().getUser().getFullName() + " on " + saved.getInterviewDate());

        return saved;
    }

    public List<Interview> getUpcomingInterviews() {
        return interviewRepository.findUpcomingInterviews(LocalDate.now());
    }

    public List<Interview> getCandidateInterviews() {
        User currentUser = authService.getCurrentUser();
        return interviewRepository.findByCandidateUserId(currentUser.getId());
    }

    public List<Interview> getAllInterviews() {
        return interviewRepository.findAll(Sort.by(Sort.Direction.DESC, "interviewDate", "interviewTime"));
    }

    public Interview getInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
    }

    @Transactional
    public Interview submitFeedback(Long id, InterviewFeedbackRequest request) {
        Interview interview = getInterviewById(id);
        interview.setFeedback(request.getFeedback());
        interview.setRating(request.getRating());
        interview.setRecommendation(request.getRecommendation());
        interview.setStatus(request.getStatus() != null ? request.getStatus() : "COMPLETED");

        Interview updated = interviewRepository.save(interview);

        auditService.log("INTERVIEW_FEEDBACK", "INTERVIEW", id,
                "Feedback submitted with rating: " + request.getRating() + " and recommendation: " + request.getRecommendation());

        return updated;
    }

    @Transactional
    public Interview updateStatus(Long id, String status) {
        Interview interview = getInterviewById(id);
        interview.setStatus(status.toUpperCase());
        Interview updated = interviewRepository.save(interview);

        auditService.log("UPDATE_INTERVIEW_STATUS", "INTERVIEW", id, "Interview status set to: " + status);
        return updated;
    }
}
