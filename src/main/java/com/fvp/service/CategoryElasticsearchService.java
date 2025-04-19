package com.fvp.service;

import com.fvp.document.CategoryDocument;
import com.fvp.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryElasticsearchService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryElasticsearchService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryDocument save(CategoryDocument category) {
        return categoryRepository.save(category);
    }

    public Optional<CategoryDocument> findById(String id) {
        return categoryRepository.findById(id);
    }

    public List<CategoryDocument> findByTenantId(Integer tenantId) {
        return categoryRepository.findByTenantId(tenantId);
    }

    public List<CategoryDocument> searchByName(String name) {
        return categoryRepository.findByNameContaining(name);
    }

    public void deleteById(String id) {
        categoryRepository.deleteById(id);
    }

    public Iterable<CategoryDocument> findAll() {
        return categoryRepository.findAll();
    }
} 