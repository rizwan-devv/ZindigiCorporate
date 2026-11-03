package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findAllByOrderBySortOrderAscNameAsc();
    List<ProductCategory> findByActiveTrueOrderBySortOrderAscNameAsc();
    Optional<ProductCategory> findByCodeIgnoreCase(String code);
}
