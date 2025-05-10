package com.fvp.repository;

import com.fvp.entity.Link;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkRepository extends JpaRepository<Link, Integer> {

  @Query(value = "SELECT l.* FROM link l WHERE l.tenant_id = :tenantId AND l.thumb_path_processed = 1 ORDER BY RAND()", nativeQuery = true)
  List<Link> findAllOrderedByRandomOrder(@Param("tenantId") Integer tenantId);

  @Query(value = "SELECT l.* FROM link l WHERE l.tenant_id = :tenantId AND l.duration <= :maxDuration AND l.thumb_path_processed = 1 ORDER BY RAND()", nativeQuery = true)
  List<Link> findByDurationLessThanEqualOrderedByRandomOrder(@Param("tenantId") Integer tenantId,
      @Param("maxDuration") Integer maxDuration);

  @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.link = :link AND l.thumbPathProcessed = 1")
  Link findByTenantIdAndLinkAndThumbPathProcessedTrue(Integer tenantId, String link);

  @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.source = :source AND l.thumbPathProcessed = 1")
  List<Link> findByTenantIdAndSourceAndThumbPathProcessedTrue(Integer tenantId, String source);

  @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.title LIKE %:keyword% AND l.thumbPathProcessed = 1")
  List<Link> findByTitleContaining(Integer tenantId, String keyword);

  @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.thumbPathProcessed = 1")
  List<Link> findByTenantId(Integer tenantId);

  // Find links by tenant ID with pagination
  Page<Link> findByTenantId(Integer tenantId, Pageable pageable);

  // Find links by date range with pagination
  Page<Link> findByCreatedOnBetween(LocalDateTime startDate, LocalDateTime endDate,
      Pageable pageable);

  // Find links by category with pagination
  Page<Link> findByCategoryContaining(String category, Pageable pageable);

  // Find a link by URL and tenant ID
  @Query("SELECT l FROM Link l WHERE l.link = :link AND l.tenantId = :tenantId AND l.thumbPathProcessed = 1")
  Link findByLinkAndTenantId(String link, Integer tenantId);

  /**
   * Find all processed links with pagination
   *
   * @param pageable Pagination information
   * @return Page of links
   */
  @Query("SELECT l FROM Link l WHERE l.thumbPathProcessed = 1 ORDER BY l.id")
  Page<Link> findAllProcessedLinks(Pageable pageable);

  /**
   * Find all distinct tenant IDs from the link table
   *
   * @return List of distinct tenant IDs
   */
  @Query(value = "SELECT DISTINCT tenant_id FROM link WHERE thumb_path_processed = 1", nativeQuery = true)
  List<Integer> findDistinctTenantIds();

  /**
   * Find processed links for a specific tenant with pagination
   *
   * @param tenantId The tenant ID
   * @param pageable Pagination information
   * @return Page of links
   */
  @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.thumbPathProcessed = 1 ORDER BY l.id")
  Page<Link> findByTenantIdProcessedLinks(@Param("tenantId") Integer tenantId, Pageable pageable);

  /**
   * Count link for a specific tenant
   *
   * @param tenantId The tenant ID
   * @return Count of link
   */
  @Query(value = "SELECT COUNT(*) AS count FROM link l WHERE l.tenant_id = :tenantId AND l.thumb_path_processed = 1", nativeQuery = true)
  Long countByTenantId(@Param("tenantId") Integer tenantId);
} 