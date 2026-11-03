package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.LoanApplication;
import com.dfs.corporate.financing.domain.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    Optional<LoanApplication> findByReferenceNo(String referenceNo);
    List<LoanApplication> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<LoanApplication> findByMerchantPartyIdOrderByCreatedAtDesc(Long merchantPartyId);
    List<LoanApplication> findAllByOrderByCreatedAtDesc();
    List<LoanApplication> findByCustomerIdAndStatusNotIn(Long customerId, Collection<LoanApplicationStatus> statuses);
}
