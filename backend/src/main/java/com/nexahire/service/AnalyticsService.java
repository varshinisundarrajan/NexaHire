package com.nexahire.service;

import com.nexahire.dto.AnalyticsSummaryDto;
import com.nexahire.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final JobRepository jobRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    public AnalyticsService(JobRepository jobRepository,
                            CandidateProfileRepository candidateProfileRepository,
                            ApplicationRepository applicationRepository,
                            InterviewRepository interviewRepository) {
        this.jobRepository = jobRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
    }

    public AnalyticsSummaryDto getAnalyticsSummary() {
        AnalyticsSummaryDto summary = new AnalyticsSummaryDto();

        summary.setTotalJobs(jobRepository.count());
        summary.setActiveJobs(jobRepository.countByStatus("ACTIVE"));
        summary.setTotalCandidates(candidateProfileRepository.count());
        summary.setTotalApplications(applicationRepository.count());
        summary.setPendingApplications(applicationRepository.countByStatus("APPLIED") + applicationRepository.countByStatus("SCREENING"));
        summary.setShortlistedCandidates(applicationRepository.countByStatus("SHORTLISTED"));
        summary.setHiredCandidates(applicationRepository.countByStatus("SELECTED"));
        summary.setUpcomingInterviews(interviewRepository.findUpcomingInterviews(LocalDate.now()).size());

        // Recruitment Funnel metrics
        Map<String, Long> funnel = new HashMap<>();
        funnel.put("APPLIED", 0L);
        funnel.put("SCREENING", 0L);
        funnel.put("SHORTLISTED", 0L);
        funnel.put("INTERVIEW", 0L);
        funnel.put("SELECTED", 0L);
        funnel.put("REJECTED", 0L);

        List<Object[]> statusCounts = applicationRepository.countApplicationsByStatus();
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            if (status != null) {
                funnel.put(status.toUpperCase(), count);
            }
        }
        summary.setFunnelData(funnel);

        // Department breakdown
        Map<String, Long> deptCounts = new HashMap<>();
        List<Object[]> deptRows = applicationRepository.countApplicationsByDepartment();
        for (Object[] row : deptRows) {
            String dept = (String) row[0];
            Long count = (Long) row[1];
            if (dept != null) {
                deptCounts.put(dept, count);
            }
        }
        summary.setDepartmentBreakdown(deptCounts);

        return summary;
    }
}
