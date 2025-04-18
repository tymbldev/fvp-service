package com.fvp.batch;

import com.fvp.entity.AllCat;
import com.fvp.model.SheetData;
import com.fvp.repository.AllCatRepository;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CategoryProcessor implements ItemProcessor<SheetData, SheetData> {

    @Autowired
    private AllCatRepository allCatRepository;

    private final LevenshteinDistance distance = new LevenshteinDistance();
    private static final double SIMILARITY_THRESHOLD = 0.8;

    @Override
    public SheetData process(SheetData item) throws Exception {
        Set<String> matchedCategories = new HashSet<>();
        
        // Process categories from the sheet
        if (item.getCategories() != null && !item.getCategories().isEmpty()) {
            String[] sheetCategories = item.getCategories().split(",");
            for (String sheetCategory : sheetCategories) {
                String trimmedCategory = sheetCategory.trim().toLowerCase();
                findMatchingCategories(trimmedCategory, matchedCategories);
            }
        }
        
        // Extract and match categories from title
        extractCategoriesFromTitle(item.getTitle(), matchedCategories);
        
        // Update the categories field with matched categories
        item.setCategories(String.join(",", matchedCategories));
        
        return item;
    }

    private void findMatchingCategories(String category, Set<String> matchedCategories) {
        List<AllCat> existingCategories = allCatRepository.findByTenantId(0); // Using default tenant
        
        for (AllCat existingCategory : existingCategories) {
            String existingName = existingCategory.getName().toLowerCase();
            
            // Calculate similarity
            int maxLength = Math.max(category.length(), existingName.length());
            double similarity = 1.0 - (double) distance.apply(category, existingName) / maxLength;
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                matchedCategories.add(existingCategory.getName());
            }
        }
    }

    private void extractCategoriesFromTitle(String title, Set<String> matchedCategories) {
        if (title == null || title.isEmpty()) {
            return;
        }
        
        // Common patterns to look for in titles
        String[] patterns = {
            "\\b(?:HD|4K|VR|POV|BBC|BWC|BGG|DP|Gangbang|Cumshot|Creampie|Anal|Blowjob|Handjob|Titjob|Facial|Squirt|Lesbian|Gay|Trans|Shemale|TS|Femdom|BDSM|MILF|Teen|Amateur|Professional|Pornstar|Celebrity)\\b",
            "\\b(?:[A-Z][a-z]+(?: [A-Z][a-z]+)*)\\b" // Words starting with capital letters
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(title);
            
            while (m.find()) {
                String potentialCategory = m.group().trim();
                findMatchingCategories(potentialCategory.toLowerCase(), matchedCategories);
            }
        }
    }
} 