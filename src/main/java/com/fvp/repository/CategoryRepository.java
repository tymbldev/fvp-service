package com.fvp.repository;

import com.fvp.document.CategoryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends ElasticsearchRepository<CategoryDocument, String> {
    List<CategoryDocument> findByTenantId(Integer tenantId);
    List<CategoryDocument> findByNameContaining(String name);
} 