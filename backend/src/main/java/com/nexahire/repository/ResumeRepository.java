package com.nexahire.repository;

import com.nexahire.entity.CandidateProfile;
import com.nexahire.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByCandidateIdOrderByUploadedAtDesc(Long candidateId);
    Optional<Resume> findFirstByCandidateIdOrderByUploadedAtDesc(Long candidateId);
}
