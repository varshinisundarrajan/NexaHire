package com.nexahire.repository;

import com.nexahire.entity.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationId(Long applicationId);

    @Query("SELECT i FROM Interview i WHERE i.application.candidate.user.id = :userId")
    List<Interview> findByCandidateUserId(@Param("userId") Long userId);

    List<Interview> findByInterviewerId(Long interviewerId);

    @Query("SELECT i FROM Interview i WHERE i.interviewDate >= :fromDate ORDER BY i.interviewDate ASC, i.interviewTime ASC")
    List<Interview> findUpcomingInterviews(@Param("fromDate") LocalDate fromDate);

    Page<Interview> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
