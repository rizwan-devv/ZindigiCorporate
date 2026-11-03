package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {
    List<LoanDocument> findByApplicationIdOrderByUploadedAtAsc(Long applicationId);
    Optional<LoanDocument> findByApplicationIdAndDocumentCode(Long applicationId, String documentCode);
}
