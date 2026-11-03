package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.FinancingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancingSettingsRepository extends JpaRepository<FinancingSettings, Long> {
}
