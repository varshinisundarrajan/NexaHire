package com.nexahire.repository;

import com.nexahire.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByStatus(String status, Pageable pageable);

    @Query("SELECT j FROM Job j WHERE " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:department IS NULL OR LOWER(j.department) = LOWER(:department)) AND " +
           "(:jobType IS NULL OR LOWER(j.jobType) = LOWER(:jobType)) AND " +
           "(:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.requiredSkills) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.location) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Job> searchJobs(@Param("search") String search,
                         @Param("department") String department,
                         @Param("jobType") String jobType,
                         @Param("status") String status,
                         Pageable pageable);

    List<Job> findByCreatedById(Long userId);

    long countByStatus(String status);

    @Query("SELECT j.department, COUNT(j) FROM Job j GROUP BY j.department")
    List<Object[]> countJobsByDepartment();
}
