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


  @Query("SELECT l FROM Link l WHERE l.tenantId = :tenantId AND l.link = :link AND l.thumbPathProcessed = 1")
  Link findByTenantIdAndLinkAndThumbPathProcessedTrue(Integer tenantId, String link);

  // Find a link by URL and tenant ID
  @Query("SELECT l FROM Link l WHERE l.link = :link AND l.tenantId = :tenantId AND l.thumbPathProcessed = 1")
  Link findByLinkAndTenantId(String link, Integer tenantId);

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
   * Count link for a specific tenant
   *
   * @param tenantId The tenant ID
   * @return Count of link
   */
  @Query(value = "SELECT COUNT(*) AS count FROM link l WHERE l.tenant_id = :tenantId AND l.thumb_path_processed = 1", nativeQuery = true)
  Long countByTenantId(@Param("tenantId") Integer tenantId);

  /**
   * Find links with a specific thumbpath and thumbPathProcessed value with pagination
   *
   * @param thumbpath The thumbpath value to search for
   * @param thumbPathProcessed The thumbPathProcessed value to search for
   * @param pageable Pagination information
   * @return Page of links
   */
  Page<Link> findByThumbpathAndThumbPathProcessed(String thumbpath, Integer thumbPathProcessed, Pageable pageable);

  /**
   * Count links with a specific thumbpath and thumbPathProcessed value
   *
   * @param thumbpath The thumbpath value to search for
   * @param thumbPathProcessed The thumbPathProcessed value to search for
   * @return Count of links
   */
  long countByThumbpathAndThumbPathProcessed(String thumbpath, Integer thumbPathProcessed);
}