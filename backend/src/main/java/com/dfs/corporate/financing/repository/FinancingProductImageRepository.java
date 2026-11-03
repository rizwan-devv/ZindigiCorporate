package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.FinancingProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancingProductImageRepository extends JpaRepository<FinancingProductImage, Long> {
    List<FinancingProductImage> findByProductIdOrderByIdAsc(Long productId);
    long countByProductId(Long productId);
}
