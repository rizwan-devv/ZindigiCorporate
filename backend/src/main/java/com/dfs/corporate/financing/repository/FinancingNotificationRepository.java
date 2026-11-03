package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.FinancingNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancingNotificationRepository extends JpaRepository<FinancingNotification, Long> {
    List<FinancingNotification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
