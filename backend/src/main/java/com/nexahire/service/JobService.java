package com.nexahire.service;

import com.nexahire.dto.JobRequest;
import com.nexahire.entity.Job;
import com.nexahire.entity.User;
import com.nexahire.exception.ResourceNotFoundException;
import com.nexahire.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final AuthService authService;
    private final AuditService auditService;

    public JobService(JobRepository jobRepository, AuthService authService, AuditService auditService) {
        this.jobRepository = jobRepository;
        this.authService = authService;
        this.auditService = auditService;
    }

    public Page<Job> searchJobs(String query, String department, String jobType, String status, int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String cleanDept = (department != null && !department.isBlank()) ? department.trim() : null;
        String cleanType = (jobType != null && !jobType.isBlank()) ? jobType.trim() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim() : null;

        return jobRepository.searchJobs(cleanQuery, cleanDept, cleanType, cleanStatus, pageable);
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job opening not found with id: " + id));
    }

    @Transactional
    public Job createJob(JobRequest request) {
        User currentUser = authService.getCurrentUser();

        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDepartment(request.getDepartment());
        job.setJobType(request.getJobType() != null ? request.getJobType() : "FULL_TIME");
        job.setLocation(request.getLocation());
        job.setExperienceMin(request.getExperienceMin());
        job.setExperienceMax(request.getExperienceMax());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setNiceToHaveSkills(request.getNiceToHaveSkills());
        job.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        job.setDeadline(request.getDeadline());
        job.setCreatedBy(currentUser);

        Job saved = jobRepository.save(job);
        auditService.log("CREATE_JOB", "JOB", saved.getId(), "Created job opening: " + saved.getTitle());
        return saved;
    }

    @Transactional
    public Job updateJob(Long id, JobRequest request) {
        Job job = getJobById(id);

        job.setTitle(request.getTitle());
        job.setDepartment(request.getDepartment());
        job.setJobType(request.getJobType() != null ? request.getJobType() : job.getJobType());
        job.setLocation(request.getLocation());
        job.setExperienceMin(request.getExperienceMin());
        job.setExperienceMax(request.getExperienceMax());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setNiceToHaveSkills(request.getNiceToHaveSkills());
        if (request.getStatus() != null) {
            job.setStatus(request.getStatus());
        }
        job.setDeadline(request.getDeadline());

        Job updated = jobRepository.save(job);
        auditService.log("UPDATE_JOB", "JOB", updated.getId(), "Updated job opening: " + updated.getTitle());
        return updated;
    }

    @Transactional
    public void deleteJob(Long id) {
        Job job = getJobById(id);
        jobRepository.delete(job);
        auditService.log("DELETE_JOB", "JOB", id, "Deleted job opening: " + job.getTitle());
    }

    @Transactional
    public Job updateJobStatus(Long id, String status) {
        Job job = getJobById(id);
        job.setStatus(status.toUpperCase());
        Job updated = jobRepository.save(job);
        auditService.log("UPDATE_JOB_STATUS", "JOB", id, "Job status changed to " + status);
        return updated;
    }

    public List<Job> getMyJobs() {
        User currentUser = authService.getCurrentUser();
        return jobRepository.findByCreatedById(currentUser.getId());
    }
}
