package com.nexahire.service;

import com.nexahire.dto.ApplicationRequest;
import com.nexahire.dto.ApplicationStatusUpdate;
import com.nexahire.dto.KeywordMatchResult;
import com.nexahire.entity.*;
import com.nexahire.exception.BadRequestException;
import com.nexahire.exception.ResourceNotFoundException;
import com.nexahire.repository.ApplicationRepository;
import com.nexahire.repository.JobRepository;
import com.nexahire.repository.ResumeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final CandidateService candidateService;
    private final ResumeParsingService resumeParsingService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobRepository jobRepository,
                              ResumeRepository resumeRepository,
                              CandidateService candidateService,
                              ResumeParsingService resumeParsingService,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.candidateService = candidateService;
        this.resumeParsingService = resumeParsingService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public Application apply(ApplicationRequest request) {
        CandidateProfile candidate = candidateService.getCurrentCandidateProfile();

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + request.getJobId()));

        if (!"ACTIVE".equalsIgnoreCase(job.getStatus())) {
            throw new BadRequestException("This job is no longer accepting applications (Status: " + job.getStatus() + ")");
        }

        if (applicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new BadRequestException("You have already submitted an application for this position");
        }

        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId()).orElse(null);
        }
        if (resume == null) {
            resume = candidateService.getLatestResume(candidate.getId());
        }

        // Calculate transparent keyword match score
        String resumeText = resume != null ? resume.getParsedText() : "";
        KeywordMatchResult matchResult = resumeParsingService.calculateMatch(
                job.getRequiredSkills(),
                candidate.getSkills(),
                resumeText,
                request.getCoverLetter()
        );

        Application application = new Application(job, candidate, resume, request.getCoverLetter());
        application.setMatchScore(matchResult.getScore());
        application.setMatchedSkills(String.join(", ", matchResult.getMatchedSkills()));
        application.setMissingSkills(String.join(", ", matchResult.getMissingSkills()));

        Application saved = applicationRepository.save(application);

        // Notify candidate
        notificationService.sendNotification(
                candidate.getUser(),
                "Application Submitted Successfully",
                "Your application for '" + job.getTitle() + "' at NexaHire has been submitted with a " + matchResult.getScore() + "% skill match score.",
                "APPLICATION_STATUS",
                "/applications.html"
        );

        // Notify recruiter / job creator if present
        if (job.getCreatedBy() != null) {
            notificationService.sendNotification(
                    job.getCreatedBy(),
                    "New Application: " + job.getTitle(),
                    candidate.getUser().getFullName() + " applied for '" + job.getTitle() + "' (Match: " + matchResult.getScore() + "%).",
                    "APPLICATION_STATUS",
                    "/applications.html?jobId=" + job.getId()
            );
        }

        auditService.log("APPLY_JOB", "APPLICATION", saved.getId(),
                "Candidate " + candidate.getUser().getFullName() + " applied for " + job.getTitle() + " (Score: " + matchResult.getScore() + "%)");

        return saved;
    }

    public Page<Application> searchApplications(Long jobId, String status, String search, int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return applicationRepository.searchApplications(jobId, status, search, pageable);
    }

    public List<Application> getApplicationsByJob(Long jobId) {
        return applicationRepository.findByJobId(jobId);
    }

    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    public List<Application> getMyApplications() {
        CandidateProfile candidate = candidateService.getCurrentCandidateProfile();
        return applicationRepository.findByCandidateId(candidate.getId());
    }

    @Transactional
    public Application updateStatus(Long id, ApplicationStatusUpdate update) {
        Application application = getApplicationById(id);
        String oldStatus = application.getStatus();
        application.setStatus(update.getStatus().toUpperCase());

        if (update.getRecruiterNotes() != null) {
            application.setRecruiterNotes(update.getRecruiterNotes());
        }

        Application updated = applicationRepository.save(application);

        // Trigger notification to candidate
        notificationService.sendNotification(
                application.getCandidate().getUser(),
                "Application Status Update: " + application.getJob().getTitle(),
                "Your application status for '" + application.getJob().getTitle() + "' has progressed to: " + application.getStatus(),
                "APPLICATION_STATUS",
                "/applications.html"
        );

        auditService.log("UPDATE_APPLICATION_STATUS", "APPLICATION", id,
                "Status changed from " + oldStatus + " to " + application.getStatus());

        return updated;
    }
}
