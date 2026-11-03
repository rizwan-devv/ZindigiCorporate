package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {
    List<LoanEvent> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
