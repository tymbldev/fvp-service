package com.fvp.repository;

import com.fvp.document.LinkDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkDocumentRepository extends ElasticsearchRepository<LinkDocument, String> {
} 