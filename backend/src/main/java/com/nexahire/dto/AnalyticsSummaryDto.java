package com.nexahire.dto;

import java.util.Map;

public class AnalyticsSummaryDto {
    private long totalJobs;
    private long activeJobs;
    private long totalCandidates;
    private long totalApplications;
    private long pendingApplications;
    private long shortlistedCandidates;
    private long upcomingInterviews;
    private long hiredCandidates;
    private Map<String, Long> funnelData;
    private Map<String, Long> departmentBreakdown;

    public AnalyticsSummaryDto() {}

    public long getTotalJobs() { return totalJobs; }
    public void setTotalJobs(long totalJobs) { this.totalJobs = totalJobs; }

    public long getActiveJobs() { return activeJobs; }
    public void setActiveJobs(long activeJobs) { this.activeJobs = activeJobs; }

    public long getTotalCandidates() { return totalCandidates; }
    public void setTotalCandidates(long totalCandidates) { this.totalCandidates = totalCandidates; }

    public long getTotalApplications() { return totalApplications; }
    public void setTotalApplications(long totalApplications) { this.totalApplications = totalApplications; }

    public long getPendingApplications() { return pendingApplications; }
    public void setPendingApplications(long pendingApplications) { this.pendingApplications = pendingApplications; }

    public long getShortlistedCandidates() { return shortlistedCandidates; }
    public void setShortlistedCandidates(long shortlistedCandidates) { this.shortlistedCandidates = shortlistedCandidates; }

    public long getUpcomingInterviews() { return upcomingInterviews; }
    public void setUpcomingInterviews(long upcomingInterviews) { this.upcomingInterviews = upcomingInterviews; }

    public long getHiredCandidates() { return hiredCandidates; }
    public void setHiredCandidates(long hiredCandidates) { this.hiredCandidates = hiredCandidates; }

    public Map<String, Long> getFunnelData() { return funnelData; }
    public void setFunnelData(Map<String, Long> funnelData) { this.funnelData = funnelData; }

    public Map<String, Long> getDepartmentBreakdown() { return departmentBreakdown; }
    public void setDepartmentBreakdown(Map<String, Long> departmentBreakdown) { this.departmentBreakdown = departmentBreakdown; }
}
