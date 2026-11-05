package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.FinancingProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancingProductRepository extends JpaRepository<FinancingProduct, Long> {
    List<FinancingProduct> findByMerchantPartyIdOrderByCreatedAtDesc(Long merchantPartyId);
    List<FinancingProduct> findAllByOrderByCreatedAtDesc();
    List<FinancingProduct> findByMerchantPartyIdAndCategoryIdAndActiveTrueAndShownToCustomersTrueOrderByTitleAsc(
            Long merchantPartyId, Long categoryId);

    List<FinancingProduct> findByMerchantPartyIdAndCategoryIdAndActiveTrueOrderByTitleAsc(
            Long merchantPartyId, Long categoryId);

    List<FinancingProduct> findByCategoryIdAndActiveTrueOrderByTitleAsc(Long categoryId);
}
