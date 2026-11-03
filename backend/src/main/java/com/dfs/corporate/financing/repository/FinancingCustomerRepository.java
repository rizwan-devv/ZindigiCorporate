package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.FinancingCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancingCustomerRepository extends JpaRepository<FinancingCustomer, Long> {
    Optional<FinancingCustomer> findByCnic(String cnic);
    Optional<FinancingCustomer> findByMobile(String mobile);
    Optional<FinancingCustomer> findBySessionToken(String sessionToken);
}
