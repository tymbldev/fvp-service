package com.fvp.repository;

import com.fvp.document.LinkDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkElasticsearchRepository extends ElasticsearchRepository<LinkDocument, String> {
    List<LinkDocument> findByCategoriesContaining(String category);
    List<LinkDocument> findByTitleContainingOrSearchableTextContaining(String title, String searchableText);
} 