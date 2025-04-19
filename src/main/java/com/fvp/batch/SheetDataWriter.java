package com.fvp.batch;

import com.fvp.entity.Link;
import com.fvp.entity.LinkCategory;
import com.fvp.model.SheetData;
import com.fvp.repository.LinkCategoryRepository;
import com.fvp.repository.LinkRepository;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SheetDataWriter implements ItemWriter<SheetData> {

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkCategoryRepository linkCategoryRepository;

    @Override
    public void write(List<? extends SheetData> items) throws Exception {
        for (SheetData item : items) {
            // Save or update link
            Link link = linkRepository.findByTenantIdAndLink(0, item.getLink());
            if (link == null) {
                link = new Link();
                link.setTenantId(0);
                link.setLink(item.getLink());
                link.setSource(item.getSource());
                link.setThumbnail(item.getThumbnail());
                link.setTitle(item.getTitle());
                link.setDuration(item.getDuration());
            }

            link = linkRepository.save(link);

            // Process categories
            if (item.getCategories() != null && !item.getCategories().isEmpty()) {
                String[] categories = item.getCategories().split(",");
                for (String category : categories) {
                    String trimmedCategory = category.trim();
                    if (!trimmedCategory.isEmpty()) {
                        LinkCategory linkCategory = new LinkCategory();
                        linkCategory.setTenantId(0);
                        linkCategory.setLink(link);
                        linkCategory.setCategory(trimmedCategory);
                        linkCategoryRepository.save(linkCategory);
                    }
                }
            }
        }
    }
} 