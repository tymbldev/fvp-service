package com.fvp.service;

import com.fvp.dto.CategoryWithLinkDTO;
import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.repository.AllCatRepository;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private AllCatRepository allCatRepository;

    @Autowired
    private LinkCategoryRepository linkCategoryRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Cacheable(value = "homeCategories", key = "#tenantId")
    public List<CategoryWithLinkDTO> getHomeCategoriesWithLinks(Integer tenantId) {
        List<CategoryWithLinkDTO> result = new ArrayList<>();
        
        // Get all categories with home=1 ordered by home_cat_order
        List<AllCat> categories = allCatRepository.findByTenantIdAndHomeOrderByHomeCatOrder(tenantId, 1);
        
        for (AllCat category : categories) {
            // Get the count of links for this category
            Long linkCount = linkCategoryRepository.countByTenantIdAndCategory(tenantId, category.getName());
            
            // Try to get a random link from the last 3 days
            Optional<LinkCategory> recentLinkCategory = linkCategoryRepository
                .findRandomRecentLinkByCategory(tenantId, category.getName());
            
            // If no recent link found, get a random link from all time
            LinkCategory selectedLinkCategory = recentLinkCategory.orElseGet(() -> 
                linkCategoryRepository.findRandomLinkByCategory(tenantId, category.getName())
                    .orElse(null)
            );
            
            if (selectedLinkCategory == null) {
                continue; // Skip if no links found
            }
            
            Link link = selectedLinkCategory.getLink();
            
            CategoryWithLinkDTO dto = new CategoryWithLinkDTO();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setHomeThumb(category.getHomeThumb());
            dto.setHeader(category.getHeader());
            dto.setHomeSEO(category.getHomeSEO());
            dto.setHomeCatOrder(category.getHomeCatOrder());
            dto.setDescription(category.getDescription());
            dto.setLink(link.getLink());
            dto.setLinkTitle(link.getTitle());
            dto.setLinkThumbnail(link.getThumbnail());
            dto.setLinkDuration(link.getDuration());
            dto.setLinkCount(linkCount);
            
            result.add(dto);
        }
        
        return result;
    }
} 