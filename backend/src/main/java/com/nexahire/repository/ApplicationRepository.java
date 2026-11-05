package com.nexahire.repository;

import com.nexahire.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByJobIdAndCandidateId(Long jobId, Long candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    Page<Application> findByJobId(Long jobId, Pageable pageable);
    List<Application> findByJobId(Long jobId);

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);
    List<Application> findByCandidateId(Long candidateId);

    Page<Application> findByStatus(String status, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE " +
           "(:jobId IS NULL OR a.job.id = :jobId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR LOWER(a.candidate.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.candidate.user.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.job.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Application> searchApplications(@Param("jobId") Long jobId,
                                         @Param("status") String status,
                                         @Param("search") String search,
                                         Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countApplicationsByStatus();

    @Query("SELECT a.job.department, COUNT(a) FROM Application a GROUP BY a.job.department")
    List<Object[]> countApplicationsByDepartment();
}
