package com.dfs.corporate.financing.repository;

import com.dfs.corporate.financing.domain.DealerAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DealerAuthorizationRepository extends JpaRepository<DealerAuthorization, Long> {
    Optional<DealerAuthorization> findByPartyIdAndCategoryId(Long partyId, Long categoryId);
    List<DealerAuthorization> findByCategoryIdAndActiveTrue(Long categoryId);
    List<DealerAuthorization> findAllByOrderByIdDesc();
}
