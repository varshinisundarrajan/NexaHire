package com.nexahire.repository;

import com.nexahire.entity.CandidateProfile;
import com.nexahire.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {
    Optional<CandidateProfile> findByUser(User user);
    Optional<CandidateProfile> findByUserId(Long userId);

    @Query("SELECT c FROM CandidateProfile c WHERE " +
           "(:keyword IS NULL OR LOWER(c.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.skills) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.headline) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.location) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CandidateProfile> searchCandidates(@Param("keyword") String keyword, Pageable pageable);
}
