package com.fvp.service;

import com.fvp.document.LinkDocument;
import com.fvp.repository.LinkElasticsearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LinkElasticsearchService {

    private final LinkElasticsearchRepository linkRepository;

    @Autowired
    public LinkElasticsearchService(LinkElasticsearchRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public LinkDocument save(LinkDocument link) {
        return linkRepository.save(link);
    }

    public Optional<LinkDocument> findById(String id) {
        return linkRepository.findById(id);
    }

    public List<LinkDocument> findByCategoriesContaining(String category) {
        return linkRepository.findByCategoriesContaining(category);
    }

    public List<LinkDocument> searchByTitleOrText(String title, String searchableText) {
        return linkRepository.findByTitleContainingOrSearchableTextContaining(title, searchableText);
    }

    public void deleteById(String id) {
        linkRepository.deleteById(id);
    }

    public Iterable<LinkDocument> findAll() {
        return linkRepository.findAll();
    }
} 