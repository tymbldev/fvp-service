package com.fvp.service;

import com.fvp.entity.AllCat;
import com.fvp.entity.Link;
import com.fvp.repository.AllCatRepository;
import com.fvp.util.Util;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import com.fvp.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class CategoryProcessingService {

  private static final Logger logger = LoggingUtil.getLogger(CategoryProcessingService.class);

  private final LinkProcessingService linkProcessingService;
  private final AllCatRepository allCatRepository;
  private final Util util;

  public CategoryProcessingService(
      LinkProcessingService linkProcessingService,
      AllCatRepository allCatRepository, Util util) {
    this.linkProcessingService = linkProcessingService;
    this.allCatRepository = allCatRepository;
    this.util = util;
  }

  public void processCategories(Link link, String categories) {
    Set<String> categorySet = new HashSet<>();
    Set<String> categoriesToCreate = new HashSet<>();

    List<AllCat> allCategories = linkProcessingService.getCategoriesForTenant(link.getTenantId());
    List<String> values = util.tokenize(categories);
    if (values == null || values.isEmpty()) {
      return;
    }
    // First pass: Collect all categories to be created
    for (String token : values) {
      String category = token.trim();
      if (!category.isEmpty()) {
        // Check if the token exactly matches any category
        boolean exactMatch = false;
        for (AllCat allCat : allCategories) {
          if (allCat.getName().equalsIgnoreCase(category)) {
            categorySet.add(allCat.getName());
            exactMatch = true;
            break;
          }
        }
        if (!exactMatch) {
          categoriesToCreate.add(category);
        }
      }
    }

    // Create new categories in a single batch
    if (!categoriesToCreate.isEmpty()) {
      Set<String> finalCategoriesToCreate = new HashSet<>(categoriesToCreate);

      for (String cat : categoriesToCreate) {
        if (cat.contains(";")) {
          finalCategoriesToCreate.addAll(Arrays.asList(cat.split(";")));
        } else {
          finalCategoriesToCreate.add(cat);
        }
      }
      categoriesToCreate = finalCategoriesToCreate;
      logger.info("Creating {} new categories in AllCat", categoriesToCreate.size());
      List<AllCat> newCategories = new ArrayList<>();

      for (String categoryName : categoriesToCreate) {
        if (categoryName.length() > 1) {
          AllCat newCategory = new AllCat();
          newCategory.setTenantId(link.getTenantId());
          newCategory.setName(categoryName);
          newCategory.setHomeThumb(false);
          newCategory.setHeader(false);
          newCategory.setHomeSEO(false);
          newCategory.setHomeCatOrder(0);
          newCategory.setHome(0);
          newCategory.setDescription(null);
          newCategory.setCreatedAt(LocalDateTime.now());
          newCategory.setCreatedViaLink(true);
          newCategories.add(newCategory);
        }
      }

      // Save all new categories in a single batch
      List<AllCat> savedCategories = allCatRepository.saveAll(newCategories);
      allCatRepository.flush(); // Explicitly flush to DB
      linkProcessingService.refreshCategoriesCache();
      logger.info("Created {} new categories in AllCat", savedCategories.size());
    }
    logger.info("Saved {} categories {} for link {}", categorySet.size(), categorySet,
        link.getId());
  }


}
