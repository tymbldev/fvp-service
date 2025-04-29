package com.fvp.repository;

import com.fvp.entity.LinkModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkModelRepository extends JpaRepository<LinkModel, Integer> {

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1")
    List<LinkModel> findByTenantIdAndModel(Integer tenantId, String model);

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1 ORDER BY RAND()")
    Optional<LinkModel> findRandomLinkByModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1 ORDER BY lm.randomOrder")
    List<LinkModel> findByTenantIdAndModelOrderByRandomOrder(Integer tenantId, String model);

    @Query("SELECT COUNT(lm) FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND lm.model = :model AND l.thumbPathProcessed = 1")
    Long countByTenantIdAndModel(@Param("tenantId") Integer tenantId, @Param("model") String model);

    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.tenantId = :tenantId AND l.thumbPathProcessed = 1")
    List<LinkModel> findByTenantId(Integer tenantId);
    
    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.linkId = :linkId AND l.thumbPathProcessed = 1")
    List<LinkModel> findByLinkId(Integer linkId);
    
    @Query("SELECT lm FROM LinkModel lm JOIN lm.link l WHERE lm.model = :model AND lm.tenantId = :tenantId AND l.thumbPathProcessed = 1")
    List<LinkModel> findByModelAndTenantId(String model, Integer tenantId);
    
    @Transactional
    void deleteByLinkId(Integer linkId);
} 