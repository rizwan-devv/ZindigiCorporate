package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.LoanDocumentRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanDocumentRuleRepository extends JpaRepository<LoanDocumentRule, Long> {
    List<LoanDocumentRule> findAllByOrderBySortOrderAsc();
    List<LoanDocumentRule> findByApplicabilityInOrderBySortOrderAsc(List<String> applicability);
}
