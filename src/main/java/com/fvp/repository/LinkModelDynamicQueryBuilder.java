package com.fvp.repository;

import com.fvp.entity.BaseLinkModel;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Central class for building dynamic queries for LinkModel entities across all shards.
 * This uses JPA Criteria API to build truly dynamic queries where conditions
 * are only included when parameters have values.
 */
@Component
public class LinkModelDynamicQueryBuilder {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Find links by tenant ID.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @return List of links for the tenant
     */
    public <T extends BaseLinkModel> List<T> findByTenantId(
            Class<T> entityClass,
            Integer tenantId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Add tenant ID predicate
        query.where(cb.equal(root.get("tenantId"), tenantId));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by link ID.
     * 
     * @param entityClass The entity class
     * @param linkId The link ID
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkModel> List<T> findByLinkId(
            Class<T> entityClass,
            Integer linkId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Add link ID predicate
        query.where(cb.equal(root.get("linkId"), linkId));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by model and tenant ID.
     * 
     * @param entityClass The entity class
     * @param model The model name
     * @param tenantId The tenant ID
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkModel> List<T> findByModelAndTenantId(
            Class<T> entityClass,
            String model,
            Integer tenantId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("model"), model));
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find links by tenant ID and model.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @return List of links matching the criteria
     */
    public <T extends BaseLinkModel> List<T> findByTenantIdAndModel(
            Class<T> entityClass,
            Integer tenantId,
            String model
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("model"), model));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Find a random link by model.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @return An optional containing a random link from the model
     */
    public <T extends BaseLinkModel> Optional<T> findRandomLinkByModel(
            Class<T> entityClass,
            Integer tenantId,
            String model
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("model"), model));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by random
        query.orderBy(cb.asc(cb.function("RAND", Double.class)));
        
        // Execute query and return first result if present
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(1);
        List<T> results = typedQuery.getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find links by tenant ID and model ordered by random order.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @return List of links matching the criteria ordered by random order
     */
    public <T extends BaseLinkModel> List<T> findByTenantIdAndModelOrderByRandomOrder(
            Class<T> entityClass,
            Integer tenantId,
            String model
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("model"), model));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by randomOrder
        query.orderBy(cb.asc(root.get("randomOrder")));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Count links by tenant ID and model.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @return The count of links in the model
     */
    public <T extends BaseLinkModel> Long countByTenantIdAndModel(
            Class<T> entityClass,
            Integer tenantId,
            String model
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(cb.equal(root.get("model"), model));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Count query
        query.select(cb.count(root));
        
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Find random links by model names.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param modelNames List of model names
     * @return List of random links from the specified models
     */
    public <T extends BaseLinkModel> List<T> findRandomLinksByModelNames(
            Class<T> entityClass,
            Integer tenantId,
            List<String> modelNames
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(root.get("model").in(modelNames));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Group by model
        query.groupBy(root.get("model"));
        
        // Order by random
        query.orderBy(cb.asc(cb.function("RAND", Double.class)));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Count links by tenant ID and models.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param modelNames List of model names
     * @return List of model name and count pairs
     */
    public <T extends BaseLinkModel> List<Object[]> countByTenantIdAndModels(
            Class<T> entityClass,
            Integer tenantId,
            List<String> modelNames
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        predicates.add(root.get("model").in(modelNames));
        predicates.add(cb.equal(linkJoin.get("thumbPathProcessed"), 1));
        
        // Apply predicates
        query.where(predicates.toArray(new Predicate[0]));
        
        // Select model and count
        query.multiselect(root.get("model"), cb.count(root));
        
        // Group by model
        query.groupBy(root.get("model"));
        
        return entityManager.createQuery(query).getResultList();
    }

    /**
     * Delete links by link ID.
     * 
     * @param entityClass The entity class
     * @param linkId The link ID
     */
    public <T extends BaseLinkModel> int deleteByLinkId(
            Class<T> entityClass,
            Integer linkId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> delete = cb.createCriteriaDelete(entityClass);
        Root<T> root = delete.from(entityClass);
        
        // Add link ID predicate
        delete.where(cb.equal(root.get("linkId"), linkId));
        
        return entityManager.createQuery(delete).executeUpdate();
    }

    /**
     * Count links by model with filters.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @param maxDuration Maximum duration (optional)
     * @param quality Quality filter (optional)
     * @return Count of matching entities
     */
    public <T extends BaseLinkModel> Long countByModelWithFilters(
            Class<T> entityClass,
            Integer tenantId,
            String model,
            Integer maxDuration,
            String quality
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates list
        List<Predicate> predicates = buildBasePredicates(cb, root, linkJoin, tenantId, model);
        
        // Add optional filters
        addDurationPredicates(cb, linkJoin, predicates, null, maxDuration);
        addQualityPredicate(cb, linkJoin, predicates, quality);
        
        // Apply predicates to query
        query.where(predicates.toArray(new Predicate[0]));
        
        // Count query
        query.select(cb.count(root));
        
        return entityManager.createQuery(query).getSingleResult();
    }

    /**
     * Find links by model with filters excluding a specific link.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @param maxDuration Maximum duration (optional)
     * @param quality Quality filter (optional)
     * @param excludeId Link ID to exclude
     * @param offset Pagination offset
     * @param limit Maximum number of results
     * @return Filtered list of entities
     */
    public <T extends BaseLinkModel> List<T> findByModelWithFiltersExcludingLinkPageable(
            Class<T> entityClass,
            Integer tenantId,
            String model,
            Integer maxDuration,
            String quality,
            Integer excludeId,
            int offset,
            int limit
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates list
        List<Predicate> predicates = buildBasePredicates(cb, root, linkJoin, tenantId, model);
        
        // Add optional filters
        addDurationPredicates(cb, linkJoin, predicates, null, maxDuration);
        addQualityPredicate(cb, linkJoin, predicates, quality);
        
        // Exclude specific link ID
        predicates.add(cb.notEqual(linkJoin.get("id"), excludeId));
        
        // Apply predicates to query
        query.where(predicates.toArray(new Predicate[0]));
        
        // Order by random_order
        query.orderBy(cb.asc(root.get("randomOrder")));
        
        // Create and execute typed query with pagination
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);
        
        return typedQuery.getResultList();
    }

    /**
     * Find links by model with filters.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @param model The model name
     * @param maxDuration Maximum duration (optional)
     * @param quality Quality filter (optional)
     * @param offset Pagination offset
     * @param limit Maximum number of results
     * @return Filtered list of entities
     */
    public <T extends BaseLinkModel> List<T> findByModelWithFiltersPageable(
            Class<T> entityClass,
            Integer tenantId,
            String model,
            Integer maxDuration,
            String quality,
            int offset,
            int limit
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        
        // Join with link table
        Join<T, Object> linkJoin = root.join("link", JoinType.INNER);
        
        // Build predicates list
        List<Predicate> predicates = buildBasePredicates(cb, root, linkJoin, tenantId, model);
        
        // Add optional filters
        addDurationPredicates(cb, linkJoin, predicates, null, maxDuration);
        addQualityPredicate(cb, linkJoin, predicates, quality);
        
        // Apply predicates to query
        query.where(predicates.toArray(new Predicate[0]));
        
        // Create and execute typed query with pagination
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);
        
        return typedQuery.getResultList();
    }

    /**
     * Find all distinct models for a tenant.
     * 
     * @param entityClass The entity class
     * @param tenantId The tenant ID
     * @return List of distinct model names
     */
    public <T extends BaseLinkModel> List<String> findAllDistinctModels(
            Class<T> entityClass,
            Integer tenantId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<T> root = query.from(entityClass);
        
        // Select distinct models
        query.select(root.get("model")).distinct(true);
        
        // Add tenant ID predicate
        query.where(cb.equal(root.get("tenantId"), tenantId));
        
        return entityManager.createQuery(query).getResultList();
    }

    // Helper methods to build query predicates
    
    private <T extends BaseLinkModel> List<Predicate> buildBasePredicates(
            CriteriaBuilder cb,
            Root<T> root,
            Join<T, Object> linkJoin,
            Integer tenantId,
            String model
    ) {
        List<Predicate> predicates = new ArrayList<>();
        
        // Required filters (always applied)
        predicates.add(cb.equal(root.get("tenantId"), tenantId));
        if(model != null && !model.isEmpty()) {
            predicates.add(cb.equal(root.get("model"), model));
        }
        
        return predicates;
    }
    
    private void addDurationPredicates(
            CriteriaBuilder cb,
            Join<?, Object> linkJoin,
            List<Predicate> predicates,
            Integer minDuration,
            Integer maxDuration
    ) {
        // Apply min duration filter only if parameter is provided
        if (minDuration != null) {
            predicates.add(cb.greaterThanOrEqualTo(linkJoin.get("duration"), minDuration));
        }
        
        // Apply max duration filter only if parameter is provided
        if (maxDuration != null) {
            predicates.add(cb.lessThanOrEqualTo(linkJoin.get("duration"), maxDuration));
        }
    }
    
    private void addQualityPredicate(
            CriteriaBuilder cb,
            Join<?, Object> linkJoin,
            List<Predicate> predicates,
            String quality
    ) {
        // Apply quality filter only if parameter is provided and not empty
        if (quality != null && !quality.isEmpty()) {
            predicates.add(cb.equal(linkJoin.get("quality"), quality));
        }
    }
} 