package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.FinancingWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancingWhitelistRepository extends JpaRepository<FinancingWhitelist, Long> {
    Optional<FinancingWhitelist> findByCnic(String cnic);
    List<FinancingWhitelist> findAllByOrderByCreatedAtDesc();
}
